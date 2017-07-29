package com.vcsajen.yourcustompaintings;

import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import com.mortennobel.imagescaling.*;
import com.mortennobel.imagescaling.AdvancedResizeOp;
import com.vcsajen.yourcustompaintings.exceptions.ImageSizeLimitExceededException;
import com.vcsajen.yourcustompaintings.util.CallableWithOneParam;
import com.vcsajen.yourcustompaintings.util.RunnableWithOneParam;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.config.DefaultConfig;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.spongepowered.api.scheduler.SpongeExecutorService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.text.format.TextColors;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Main plugin class
 * Created by VcSaJen on 26.07.2017 17:21.
 */
@Plugin(id = "yourcustompaintings", name = "YourCustomPaintings", description = "Upload your own custom paintings to minecraft server!")
public class YourCustomPaintings {
    @Inject
    private Logger logger;

    @Inject
    private Game game;

    @Inject
    private PluginContainer myPlugin;

    @Inject
    @DefaultConfig(sharedRoot = true)
    private ConfigurationLoader<CommentedConfigurationNode> configManager;

    private YcpConfig myConfig;

    private class UploadPaintingParams
    {
        private MessageChannel messageChannel;
        private String url;
        private int mapsX;
        private int mapsY;
        private ScaleMode scaleMode;
        private AdvancedResizeOp.UnsharpenMask unsharpenMask;

        public MessageChannel getMessageChannel() {
            return messageChannel;
        }

        public String getUrl() {
            return url;
        }

        public int getMapsX() {
            return mapsX;
        }

        public int getMapsY() {
            return mapsY;
        }

        public ScaleMode getScaleMode() {
            return scaleMode;
        }

        public AdvancedResizeOp.UnsharpenMask getUnsharpenMask() {
            return unsharpenMask;
        }

        public UploadPaintingParams(MessageChannel messageChannel, String url, int mapsX, int mapsY, ScaleMode scaleMode, AdvancedResizeOp.UnsharpenMask unsharpenMask) {
            this.messageChannel = messageChannel;
            this.url = url;
            this.mapsX = mapsX;
            this.mapsY = mapsY;
            this.scaleMode = scaleMode;
            this.unsharpenMask = unsharpenMask;
        }
    }

    private enum ScaleMode {
        NoScale,
        BSpline,
        Bell,
        BiCubic,
        BiCubicHighFreqResponse,
        BoxFilter,
        Hermite,
        Lanczos3,
        Mitchell,
        Triangle
    }

    private static void printImgInCenter(BufferedImage printOn, BufferedImage whatToPrint)
    {
        Graphics2D printOnImgGraphics = printOn.createGraphics();
        try {
            Color oldColor = printOnImgGraphics.getColor();
            printOnImgGraphics.setPaint(new Color(255,255,255,0));
            printOnImgGraphics.fillRect(0, 0, printOn.getWidth(), printOn.getHeight());
            printOnImgGraphics.setColor(oldColor);
            printOnImgGraphics.drawImage(whatToPrint, null, printOn.getWidth()/2-whatToPrint.getWidth()/2, printOn.getHeight()/2 - whatToPrint.getHeight()/2);
        } finally {
            printOnImgGraphics.dispose();
        }
    }

    private void runUploadPaintingTask(UploadPaintingParams params) {
        SpongeExecutorService minecraftExecutor = Sponge.getScheduler().createSyncExecutor(myPlugin);
        URLConnection conn = null;
        try {
            BufferedImage img;
            URL url = new URL(params.getUrl());

            if (url.getProtocol() == null || !(url.getProtocol().equals("http") || url.getProtocol().equals("https") || url.getProtocol().equals("ftp")))
                throw new MalformedURLException("Wrong URL protocol, only http(s) and ftp supported!");
            conn = url.openConnection();
            // now you get the content length
            int cLength = conn.getContentLength();
            if (cLength > myConfig.getMaxImgFileSize()) throw new ImageSizeLimitExceededException();
            long startTimer = System.nanoTime();
            //byte[] outBytes = new byte[myConfig.getMaxImgFileSize()];
            try (InputStream httpStream = conn.getInputStream();
                 ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream())
            {
                //int readBytesCount = httpStream.read(outBytes, 0, myConfig.getMaxImgFileSize());
                int nRead;
                byte[] data = new byte[16384];
                while ((nRead = httpStream.read(data, 0, data.length)) != -1) {
                    byteArrayOutputStream.write(data, 0, nRead);
                    if (byteArrayOutputStream.size() > myConfig.getMaxImgFileSize())
                        throw new ImageSizeLimitExceededException();
                    long endTimer = System.nanoTime();
                    if (TimeUnit.NANOSECONDS.toMillis(endTimer - startTimer) >= myConfig.getProgressReportTime()) {
                        startTimer = endTimer;
                        params.getMessageChannel().send(Text.of("Image download progress: " + (cLength>0 ? (100*byteArrayOutputStream.size()/cLength + "%") : (byteArrayOutputStream.size()+" bytes"))));
                    }
                }
                byteArrayOutputStream.flush();

                if (httpStream.read()!=-1) throw new ImageSizeLimitExceededException();

                try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray()))
                {
                    BufferedImage rawImg = ImageIO.read(byteArrayInputStream);
                    img = new BufferedImage(rawImg.getWidth(), rawImg.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
                    img.getGraphics().drawImage(rawImg, 0, 0, null);
                    params.getMessageChannel().send(Text.of("Image dimensions: "+img.getWidth()+"x"+img.getHeight()));
                }
            }

            ((HttpURLConnection)conn).disconnect();
            conn = null;
            params.getMessageChannel().send(Text.of("Image was downloaded successfully. Scaling…"));
            BufferedImage scaledImg = new BufferedImage(128*params.getMapsX(), 128*params.getMapsY(), BufferedImage.TYPE_4BYTE_ABGR);

            double imgAspectRatio = 1.0d*img.getWidth()/img.getHeight();
            double mapsAspectRatio = 1.0d*scaledImg.getWidth()/scaledImg.getHeight();

            int scaledW = imgAspectRatio>mapsAspectRatio ? scaledImg.getWidth() : (int)Math.round(scaledImg.getHeight()*imgAspectRatio);
            int scaledH = imgAspectRatio>mapsAspectRatio ? (int)Math.round(scaledImg.getWidth()/imgAspectRatio) : scaledImg.getHeight();
            BufferedImage rawScaledImg = new BufferedImage(scaledW, scaledH, BufferedImage.TYPE_4BYTE_ABGR);
            if (params.getScaleMode()!=ScaleMode.NoScale) {
                ResampleOp resampleOp = new ResampleOp(rawScaledImg.getWidth(), rawScaledImg.getHeight());
                resampleOp.setUnsharpenMask(params.getUnsharpenMask());
                ResampleFilter filter = ResampleFilters.getLanczos3Filter();
                switch (params.getScaleMode()) {
                    case BSpline:
                        filter = ResampleFilters.getBSplineFilter();
                        break;
                    case Bell:
                        filter = ResampleFilters.getBellFilter();
                        break;
                    case BiCubic:
                        filter = ResampleFilters.getBiCubicFilter();
                        break;
                    case BiCubicHighFreqResponse:
                        filter = ResampleFilters.getBiCubicHighFreqResponse();
                        break;
                    case BoxFilter:
                        filter = ResampleFilters.getBoxFilter();
                        break;
                    case Hermite:
                        filter = ResampleFilters.getHermiteFilter();
                        break;
                    case Lanczos3:
                        filter = ResampleFilters.getLanczos3Filter();
                        break;
                    case Mitchell:
                        filter = ResampleFilters.getMitchellFilter();
                        break;
                    case Triangle:
                        filter = ResampleFilters.getTriangleFilter();
                        break;
                }
                resampleOp.setFilter(filter);
                resampleOp.filter(img, rawScaledImg);
                printImgInCenter(scaledImg, rawScaledImg);


            } else {
                printImgInCenter(scaledImg, img);
            }
            ImageIO.write(scaledImg, "png", new File("imagae456.png")); //TODO: Удалить

            params.getMessageChannel().send(Text.of("Converting to map palette…"));








            List<Future<String>> futuresGetLastMapInd = minecraftExecutor.invokeAll(Collections.singleton(new CallableWithOneParam<String,String>("test", s -> {
                params.getMessageChannel().send(Text.of("Generating map…"));

                return "returntest";
            })));

            List<Future<String>> futuresSetLastMapInd = minecraftExecutor.invokeAll(Collections.singleton(new CallableWithOneParam<String,String>("test", s -> {

                return "returntest";
            })));
            String s = futuresSetLastMapInd.get(0).get();


            params.getMessageChannel().send(Text.of("Success!"));
        } catch (InterruptedException | ExecutionException ex) {
            params.getMessageChannel().send(Text.of(TextColors.RED, "Upload of a painting was interrupted!"));
            logger.error("Upload of a painting was interrupted!", ex);
        } catch (MalformedURLException ex) {
            params.getMessageChannel().send(Text.of(TextColors.RED, "Image URL is malformed!"));
            logger.debug("URL is malformed!", ex);
        } catch (IOException ex) {
            params.getMessageChannel().send(Text.of(TextColors.RED, "Couldn't download image! Make sure you entered correct URL."));
            logger.debug("IOException while uploading painting!", ex);
        } catch (ImageSizeLimitExceededException ex) {
            params.getMessageChannel().send(Text.of(TextColors.RED, ex.getMessage()));
            logger.debug("Image file size was too big while uploading painting!", ex);
        } catch (Exception ex) {
            params.getMessageChannel().send(Text.of(TextColors.RED, "Unknown error ("+ex.getClass().getSimpleName()+") occured while uploading painting: "+ex.getMessage()));
            throw ex;
        }
        finally {
            if (conn!=null)
                ((HttpURLConnection)conn).disconnect();
        }
    }


    private CommandResult cmdMyTest(CommandSource cmdSource, CommandContext commandContext) {
        String url = commandContext.<String>getOne("URL").get();
        int mapsX = commandContext.<Integer>getOne("MapsX").orElse(1);
        int mapsY = commandContext.<Integer>getOne("MapsY").orElse(1);
        ScaleMode scaleMode = commandContext.<ScaleMode>getOne("ScaleMode").get();
        AdvancedResizeOp.UnsharpenMask unsharpenMask = commandContext.<AdvancedResizeOp.UnsharpenMask>getOne("UnsharpenMode").orElse(AdvancedResizeOp.UnsharpenMask.None);

        cmdSource.sendMessage(Text.of("Downloading "+url+"…"));

        Task task = Task.builder().execute(new RunnableWithOneParam<UploadPaintingParams>(
                new UploadPaintingParams(cmdSource.getMessageChannel(), url, mapsX, mapsY, scaleMode, unsharpenMask), this::runUploadPaintingTask))
                .async()
                .submit(myPlugin);

        cmdSource.getMessageChannel().send(Text.of("Success!"));
        cmdSource.sendMessage(Text.of("Success!"));
        return CommandResult.success();
    }

    @Listener
    public void onInit(GamePreInitializationEvent event) {
        //-----------
        //Config registration
        ConfigurationNode rootNode;
        try {
            rootNode = configManager.load();

            myConfig = rootNode.getValue(TypeToken.of(YcpConfig.class), new YcpConfig());
            rootNode.setValue(TypeToken.of(YcpConfig.class), myConfig);

            try {
                configManager.save(rootNode);
            } catch(IOException e) {
                logger.error("Couldn't save configuration!", e);
            }
        } catch(IOException e) {
            myConfig = new YcpConfig();
            logger.error("Couldn't load configuration!", e);
        } catch (ObjectMappingException e) {
            logger.error("Some Configurate mapping exception. This shouldn't happen", e);
        }
        //-----------
        //Command registration
        CommandSpec uploadPaintingCmdSpec = CommandSpec.builder()
                .description(Text.of("Upload painting from web"))
                .extendedDescription(Text.of("Enter URL of a picture, map(s) containing painting will be generated"))
                .arguments(GenericArguments.string(Text.of("URL")),
                        GenericArguments.optional(GenericArguments.seq(GenericArguments.integer(Text.of("MapsX")), GenericArguments.integer(Text.of("MapsY")))),
                        GenericArguments.optional(GenericArguments.enumValue(Text.of("ScaleMode"), ScaleMode.class), ScaleMode.Lanczos3)/*,
                        GenericArguments.optional(GenericArguments.enumValue(Text.of("UnsharpenMode"), UnsharpenMask.class), UnsharpenMask.None)*/)
                .executor(this::cmdMyTest)
                .build();
        game.getCommandManager().register(this, uploadPaintingCmdSpec, "uploadpainting", "up-p");
    }

    @Listener
    public void onServerStart(GameStartedServerEvent event) {
        // Hey! The server has started!
        // Try instantiating your logger in here.
        // (There's a guide for that)
        logger.debug("*************************");
        logger.debug("HI! MY PLUGIN IS WORKING!");
        logger.debug("*************************");
        logger.debug("MaxImgFileSize: "+myConfig.getMaxImgFileSize());
    }
}

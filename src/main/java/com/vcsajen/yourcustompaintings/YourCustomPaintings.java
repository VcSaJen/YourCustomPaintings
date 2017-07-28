package com.vcsajen.yourcustompaintings;

import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
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
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

        public MessageChannel getMessageChannel() {
            return messageChannel;
        }

        public String getUrl() {
            return url;
        }

        public UploadPaintingParams(MessageChannel messageChannel, String url) {
            this.messageChannel = messageChannel;
            this.url = url;
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
                    img = ImageIO.read(byteArrayInputStream);
                    params.getMessageChannel().send(Text.of("Image dimensions: "+img.getWidth()+"x"+img.getHeight()));
                }
            }

            ((HttpURLConnection)conn).disconnect();
            conn = null;
            params.getMessageChannel().send(Text.of("Image was downloaded successfully. Scaling…"));
            
            params.getMessageChannel().send(Text.of("Converting to map palette…"));

            List<Future<String>> futures = minecraftExecutor.invokeAll(Collections.singleton(new CallableWithOneParam<String,String>("test", s -> {
                params.getMessageChannel().send(Text.of("Generating map…"));

                return "returntest";
            })));
            String s = futures.get(0).get();


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
        } finally {
            if (conn!=null)
                ((HttpURLConnection)conn).disconnect();
        }
    }


    private CommandResult cmdMyTest(CommandSource cmdSource, CommandContext commandContext) {
        String url = commandContext.<String>getOne("URL").get();
        cmdSource.sendMessage(Text.of("Downloading "+url+"…"));

        Task task = Task.builder().execute(new RunnableWithOneParam<UploadPaintingParams>(
                new UploadPaintingParams(cmdSource.getMessageChannel(), url), this::runUploadPaintingTask))
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
                .arguments(GenericArguments.string(Text.of("URL")))
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

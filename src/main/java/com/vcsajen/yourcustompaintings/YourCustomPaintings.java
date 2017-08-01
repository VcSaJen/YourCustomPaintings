package com.vcsajen.yourcustompaintings;

import com.flowpowered.nbt.ByteArrayTag;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.ShortTag;
import com.flowpowered.nbt.Tag;
import com.flowpowered.nbt.stream.NBTInputStream;
import com.flowpowered.nbt.stream.NBTOutputStream;
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
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.world.ConstructWorldPropertiesEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
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

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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

    @Inject
    @ConfigDir(sharedRoot = false)
    private Path configDir;

    private YcpConfig myConfig;

    @SuppressWarnings("WeakerAccess")
    private class UploadPaintingParams
    {
        @Nullable
        private UUID callerPlr;
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

        public Optional<UUID> getCallerPlr() {
            return Optional.ofNullable(callerPlr);
        }

        public UploadPaintingParams(@Nullable UUID callerPlr, MessageChannel messageChannel, String url, int mapsX, int mapsY, ScaleMode scaleMode, AdvancedResizeOp.UnsharpenMask unsharpenMask) {
            this.callerPlr = callerPlr;
            this.messageChannel = messageChannel;
            this.url = url;
            this.mapsX = mapsX;
            this.mapsY = mapsY;
            this.scaleMode = scaleMode;
            this.unsharpenMask = unsharpenMask;
        }
    }

    private class RegisterMapParams
    {
        @Nullable
        private UUID callerPlr;
        private MessageChannel messageChannel;
        private String tmpId;
        private int tileCount;

        public Optional<UUID> getCallerPlr() {
            return Optional.ofNullable(callerPlr);
        }

        public MessageChannel getMessageChannel() {
            return messageChannel;
        }

        public String getTmpId() {
            return tmpId;
        }

        public int getTileCount() {
            return tileCount;
        }

        public RegisterMapParams(@Nullable UUID callerPlr, MessageChannel messageChannel, String tmpId, int tileCount) {
            this.callerPlr = callerPlr;
            this.messageChannel = messageChannel;
            this.tmpId = tmpId;
            this.tileCount = tileCount;
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

    private final int[] mapIndexedColors = {0x00000000, 0x00000000, 0x00000000, 0x00000000, 0xFF5A7E28, 0xFF6E9A30, 0xFF7FB238, 0xFF435E1E,
            0xFFAEA473, 0xFFD5C98D, 0xFFF7E9A3, 0xFF837B56, 0xFF8C8C8C, 0xFFACACAC, 0xFFC7C7C7, 0xFF696969,
            0xFFB40000, 0xFFDC0000, 0xFFFF0000, 0xFF870000, 0xFF7171B4, 0xFF8A8ADC, 0xFFA0A0FF, 0xFF555587,
            0xFF767676, 0xFF909090, 0xFFA7A7A7, 0xFF585858, 0xFF005800, 0xFF006B00, 0xFF007C00, 0xFF004200,
            0xFFB4B4B4, 0xFFDCDCDC, 0xFFFFFFFF, 0xFF878787, 0xFF747782, 0xFF8D919F, 0xFFA4A8B8, 0xFF575961,
            0xFF6B4D36, 0xFF825E42, 0xFF976D4D, 0xFF503A29, 0xFF4F4F4F, 0xFF616161, 0xFF707070, 0xFF3B3B3B,
            0xFF2D2DB4, 0xFF3737DC, 0xFF4040FF, 0xFF222287, 0xFF655433, 0xFF7B673E, 0xFF8F7748, 0xFF4C3F26,
            0xFFB4B2AD, 0xFFDCD9D3, 0xFFFFFCF5, 0xFF878582, 0xFF985A24, 0xFFBA6E2C, 0xFFD87F33, 0xFF72431B,
            0xFF7E3698, 0xFF9A42BA, 0xFFB24CD8, 0xFF5E2872, 0xFF486C98, 0xFF5884BA, 0xFF6699D8, 0xFF365172,
            0xFFA2A224, 0xFFC6C62C, 0xFFE5E533, 0xFF79791B, 0xFF5A9012, 0xFF6EB016, 0xFF7FCC19, 0xFF436C0D,
            0xFFAB5A74, 0xFFD16E8E, 0xFFF27FA5, 0xFF804357, 0xFF363636, 0xFF424242, 0xFF4C4C4C, 0xFF282828,
            0xFF6C6C6C, 0xFF848484, 0xFF999999, 0xFF515151, 0xFF365A6C, 0xFF426E84, 0xFF4C7F99, 0xFF284351,
            0xFF5A2C7E, 0xFF6E369A, 0xFF7F3FB2, 0xFF43215E, 0xFF24367E, 0xFF2C429A, 0xFF334CB2, 0xFF1B285E,
            0xFF483624, 0xFF58422C, 0xFF664C33, 0xFF36281B, 0xFF485A24, 0xFF586E2C, 0xFF667F33, 0xFF36431B,
            0xFF6C2424, 0xFF842C2C, 0xFF993333, 0xFF511B1B, 0xFF121212, 0xFF161616, 0xFF191919, 0xFF0D0D0D,
            0xFFB0A836, 0xFFD8CD42, 0xFFFAEE4D, 0xFF847E29, 0xFF419B96, 0xFF4FBDB8, 0xFF5CDBD5, 0xFF317471,
            0xFF345AB4, 0xFF406EDC, 0xFF4A80FF, 0xFF274487, 0xFF009929, 0xFF00BB32, 0xFF00D93A, 0xFF00731F,
            0xFF5B3D23, 0xFF6F4A2A, 0xFF815631, 0xFF442E1A, 0xFF4F0100, 0xFF610200, 0xFF700200, 0xFF3B0100,
            0xFF947D72, 0xFFB4998B, 0xFFD1B1A1, 0xFF6F5E55, 0xFF703A19, 0xFF89471F, 0xFF9F5224, 0xFF542B13,
            0xFF693D4C, 0xFF814B5D, 0xFF95576C, 0xFF4F2E39, 0xFF4F4C61, 0xFF615D77, 0xFF706C8A, 0xFF3B3949,
            0xFF835E19, 0xFFA0731F, 0xFFBA8524, 0xFF624613, 0xFF495325, 0xFF59652E, 0xFF677535, 0xFF373E1C,
            0xFF713637, 0xFF8A4243, 0xFFA04D4E, 0xFF552929, 0xFF281D19, 0xFF31231E, 0xFF392923, 0xFF1E1613,
            0xFF5F4C45, 0xFF745C55, 0xFF876B62, 0xFF473934, 0xFF3D4141, 0xFF4B4F4F, 0xFF575C5C, 0xFF2E3131,
            0xFF56343E, 0xFF693F4C, 0xFF7A4958, 0xFF41272F, 0xFF362C41, 0xFF42354F, 0xFF4C3E5C, 0xFF282131,
            0xFF362319, 0xFF422B1E, 0xFF4C3223, 0xFF281A13, 0xFF363A1E, 0xFF424724, 0xFF4C522A, 0xFF282B16,
            0xFF642A20, 0xFF7B3428, 0xFF8E3C2E, 0xFF4B2018};

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

    private Path dbgDir;
    private RandomStringGenerator randomStringGenerator;

    //https://stackoverflow.com/questions/6334311/whats-the-best-way-to-round-a-color-object-to-the-nearest-color-constant
    static double colorDistance(Color c1, Color c2)
    {
        int red1 = c1.getRed();
        int red2 = c2.getRed();
        int rmean = (red1 + red2) >> 1;
        int r = red1 - red2;
        int g = c1.getGreen() - c2.getGreen();
        int b = c1.getBlue() - c2.getBlue();
        return Math.sqrt((((512+rmean)*r*r)>>8) + 4*g*g + (((767-rmean)*b*b)>>8));
    }

    static double colorDistance(int R1, int G1, int B1, int R2, int G2, int B2)
    {
        int rmean = (R1 + R2) >> 1;
        int r = R1 - R2;
        int g = G1 - G2;
        int b = B1 - B2;
        return Math.sqrt((((512+rmean)*r*r)>>8) + 4*g*g + (((767-rmean)*b*b)>>8));
    }

    int closestMapColorIndex(int R, int G, int B)
    {
        int closestIndex = 0;
        double closestDistance = 1E100;
        for (int i=4; i<mapIndexedColors.length; i++) { //0-3 are reserved for transparent
            double curDist = colorDistance(R,G,B,(mapIndexedColors[i]&0x00FF0000)>>16,(mapIndexedColors[i]&0x0000FF00)>>8,mapIndexedColors[i]&0x000000FF);
            //double curDist = colorDistance(new Color(R,G,B), new Color(mapIndexedColors[i]));
            if (curDist<closestDistance) {
                closestIndex = i;
                closestDistance = curDist;
            }
        }
        return closestIndex;
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
            if (myConfig.isDebugMode()) {
                ImageIO.write(scaledImg, "png", dbgDir.resolve("zzz_scaled_fullcolor_nontiled_img.png").toFile());

                File[] tmpTileImgFiles = (dbgDir.toFile().listFiles((dir, name) -> name.matches( "scaled_mapcolor_tile_.*_img\\.png" )));
                if (tmpTileImgFiles!=null)
                    for ( final File file : tmpTileImgFiles ) {
                        if ( !file.delete() ) {
                            logger.error( "Can't remove " + file.getAbsolutePath() );
                        }
                    }
            }

            params.getMessageChannel().send(Text.of("Converting to map palette…"));
            final String tmpId = randomStringGenerator.generate();
            final Path dataFolder = Sponge.getGame().getSavesDirectory().resolve(Sponge.getServer().getDefaultWorldName()).resolve("data");

            File[] tmpMapDatFiles = (dataFolder.toFile().listFiles((dir, name) -> name.matches( "map_tmp_.*\\.dat" )));
            if (tmpMapDatFiles!=null)
                for ( final File file : tmpMapDatFiles ) {
                    if ( !file.delete() ) {
                        logger.error( "Can't remove " + file.getAbsolutePath() );
                    }
                }


            byte[] mapData = new byte[128*128];

            for (int k=0; k<params.getMapsX(); k++) {
                for (int l=0; l<params.getMapsY(); l++) {
                    //BufferedImage mapImgIn = scaledImg.getSubimage(k*128,l*128,128,128);
                    BufferedImage mapImgIn = new BufferedImage(128, 128, BufferedImage.TYPE_4BYTE_ABGR);
                    Graphics2D mapImgInGraphics = mapImgIn.createGraphics();
                    //Color oldColor = mapImgInGraphics.getColor();
                    //mapImgInGraphics.setPaint(new Color(255,255,255,0));
                    //mapImgInGraphics.fillRect(0, 0, mapImgIn.getWidth(), mapImgIn.getHeight());
                    //mapImgInGraphics.setColor(oldColor);
                    mapImgInGraphics.drawImage(scaledImg, -k*128, -l*128, null);
                    mapImgInGraphics.dispose();
                    BufferedImage mapImgOut = new BufferedImage(128, 128, BufferedImage.TYPE_4BYTE_ABGR);
                    byte[] pixels = ((DataBufferByte) mapImgIn.getRaster().getDataBuffer()).getData();
                    byte[] outPixels = ((DataBufferByte) mapImgOut.getRaster().getDataBuffer()).getData();

                    for (int i=0; i<mapImgIn.getHeight(); i++) {
                        for (int j = 0; j < mapImgIn.getWidth(); j++) {
                            int w = mapImgIn.getWidth();
                            boolean opaque = (pixels[i*w*4+j*4+0] & 0xFF) > 128;
                            int colorIndex = closestMapColorIndex(pixels[i*w*4+j*4+3]&0xFF, pixels[i*w*4+j*4+2]&0xFF, pixels[i*w*4+j*4+1]&0xFF);
                            mapData[i*w+j] = opaque ? (byte)colorIndex : 0;
                            outPixels[i*w*4+j*4+0] = opaque ? (byte)255 : 0;
                            outPixels[i*w*4+j*4+3] = (byte) ((mapIndexedColors[colorIndex]>>16)&0xFF);
                            outPixels[i*w*4+j*4+2] = (byte) ((mapIndexedColors[colorIndex]>>8)&0xFF);
                            outPixels[i*w*4+j*4+1] = (byte) (mapIndexedColors[colorIndex]&0xFF);
                        }
                    }
                    PixelInterleavedSampleModel sampleModel = (PixelInterleavedSampleModel)mapImgOut.getRaster().getSampleModel();
                    if (myConfig.isDebugMode()) {
                        mapImgOut = new BufferedImage(mapImgOut.getColorModel(),
                                Raster.createInterleavedRaster(new DataBufferByte(outPixels, outPixels.length), mapImgOut.getWidth(), mapImgOut.getHeight(), sampleModel.getScanlineStride(), sampleModel.getPixelStride(), sampleModel.getBandOffsets(), null),
                                mapImgOut.isAlphaPremultiplied(),
                                null);

                        ImageIO.write(mapImgOut, "png", dbgDir.resolve("scaled_mapcolor_tile_"+l+"_"+k+"_img.png").toFile());
                    }
                    //Генерируем сами файлы карт
                    Path fileName = dataFolder.resolve("map_tmp_"+tmpId+"_"+(k+l*params.getMapsX())+".dat");
                    myPlugin.getAsset("map_N.dat").orElseThrow(() -> new IOException("Asset map_N.dat not found"))
                            .copyToFile(fileName);

                    CompoundTag root;
                    try (InputStream fis = new FileInputStream(fileName.toFile());
                         NBTInputStream nbtInputStream = new NBTInputStream(fis, true)) {
                        root = (CompoundTag)nbtInputStream.readTag();
                    }
                    ((CompoundTag)root.getValue().get("data")).getValue().put(new ByteArrayTag("colors", mapData));
                    try (OutputStream fos = new FileOutputStream(fileName.toFile());
                         NBTOutputStream nbtOutputStream = new NBTOutputStream(fos, true)) {
                        nbtOutputStream.writeTag(root);
                    }
                }
            }

            List<Future<Boolean>> futuresGetLastMapInd = minecraftExecutor.invokeAll(Collections.singleton(new CallableWithOneParam<RegisterMapParams,Boolean>(
                    new RegisterMapParams(params.getCallerPlr().orElse(null), params.getMessageChannel(), tmpId, params.getMapsX()*params.getMapsY()), regMapParams -> {
                params.getMessageChannel().send(Text.of("Generating map…"));
                try {
                    Path idCountsPath = dataFolder.resolve("idcounts.dat");
                    if (Files.notExists(idCountsPath))
                        myPlugin.getAsset("idcounts.dat").orElseThrow(() -> new IOException("Asset idcounts.dat not found"))
                                .copyToFile(idCountsPath);

                    CompoundTag root;
                    try (InputStream fis = new FileInputStream(idCountsPath.toFile());
                         NBTInputStream nbtInputStream = new NBTInputStream(fis, false)) {
                        root = (CompoundTag) nbtInputStream.readTag();
                    }
                    final int lastMapId = ((ShortTag)root.getValue().get("map")).getValue();
                    if (lastMapId+regMapParams.getTileCount()>Short.MAX_VALUE) {
                        regMapParams.getMessageChannel().send(Text.of(TextColors.RED, "Map ID limit exceed! (projected: "+(lastMapId+regMapParams.getTileCount())+", max: "+Short.MAX_VALUE+")"));
                        return false;
                    }
                    root.getValue().put(new ShortTag("map", (short)(lastMapId+regMapParams.getTileCount())));

                    for (int i=0;i<regMapParams.getTileCount();i++) {
                        Path mapFileName = dataFolder.resolve("map_tmp_"+tmpId+"_"+i+".dat");
                        Path newMapFileName = dataFolder.resolve("map_"+(lastMapId+1+i)+".dat");
                        Files.move(mapFileName, newMapFileName, StandardCopyOption.REPLACE_EXISTING);
                    }

                    try (OutputStream fos = new FileOutputStream(idCountsPath.toFile());
                         NBTOutputStream nbtOutputStream = new NBTOutputStream(fos, false)) {
                        nbtOutputStream.writeTag(root);
                    }
                    if (regMapParams.getCallerPlr().isPresent()) {
                        Sponge.getGame().getServer().getPlayer(regMapParams.getCallerPlr().get()).ifPresent(player -> {
                            for (int i=0;i<regMapParams.getTileCount();i++) {
                                ItemStack itemStack = ItemStack.builder().itemType(ItemTypes.FILLED_MAP).quantity(1).build();
                                DataView rawData = itemStack.toContainer();
                                rawData.set(DataQuery.of("UnsafeDamage"), lastMapId+1+i);
                                itemStack = ItemStack.builder().fromContainer(rawData).build();
                                player.getInventory().offer(itemStack);
                            }
                        });
                    }
                } catch (IOException e) {
                    regMapParams.getMessageChannel().send(Text.of(TextColors.RED, "IO Error while performing actual map rename/registration"));
                    logger.error("IO Error in futuresGetLastMapInd!", e);
                    return false;
                }
                return true;
            })));

            boolean success = futuresGetLastMapInd.get(0).get();

            if (success)
                params.getMessageChannel().send(Text.of("Painting was successfully uploaded!"));
            else params.getMessageChannel().send(Text.of(TextColors.RED, "Upload of a painting failed!"));

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
                new UploadPaintingParams(Player.class.isInstance(cmdSource) ? ((Player)cmdSource).getUniqueId() : null, cmdSource.getMessageChannel(), url, mapsX, mapsY, scaleMode, unsharpenMask), this::runUploadPaintingTask))
                .async()
                .submit(myPlugin);

        cmdSource.getMessageChannel().send(Text.of("..."));
        return CommandResult.success();
    }

    @Listener
    public void onConstructWorld(ConstructWorldPropertiesEvent event) {
        logger.debug("Saves directory: "+String.valueOf(game.getSavesDirectory()));
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
        //-----------
        //Other
        dbgDir = configDir.resolve("debug");
        if (myConfig.isDebugMode()) {
            File directory = dbgDir.toFile();
            if (!directory.exists()) {
                directory.mkdirs();
                // If you require it to make the entire directory path including parents,
                // use directory.mkdirs(); here instead.
            }
        }
        randomStringGenerator = new RandomStringGenerator(8);
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

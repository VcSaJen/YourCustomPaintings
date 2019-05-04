# YourCustomPaintings
[![Build Status](https://travis-ci.org/VcSaJen/YourCustomPaintings.svg?branch=master)](https://travis-ci.org/VcSaJen/YourCustomPaintings) [![Latest Stable Version](https://img.shields.io/github/release/VcSaJen/YourCustomPaintings.svg)](https://github.com/VcSaJen/YourCustomPaintings/releases/latest "Latest Stable Version") [![Download Dev Version](https://api.bintray.com/packages/vcsajen/generic/YourCustomPaintings/images/download.svg)](https://bintray.com/vcsajen/generic/YourCustomPaintings "Download Dev Version")

Upload your own custom paintings to minecraft server!

YourCustomPaintings is a Sponge plugin for uploading your own custom paintings to server. Paintings are implemented through minecraft maps+item frames. You can upload paintings spanning across multiple maps/frames, with convenient tool for automatic placing of them. Image will be either scaled automatically, or placed on center, with or without dithering. Various options of scaling and dithering modes are available. Once you uploaded a painting, it will be always available to you from the list. Players in survival will be required to have needed items, like item frames and empty maps (Can be turned off with permissions). Permissions and Permission Options are supported, and simple config file too.

**Commands**  
List of available commands and their parameters (parameters in square brackets are optional):  
`/uploadpainting [Player] <Name> <URL> [<MapsX> <MapsY>] [ScaleMode] [DitherMode] [ColorBleedReductionPercent]` - Uploads a painting and gives you either a filled map (for 1x1 painting) or painting placer (more about painting placer is in it's own section)  
`Player` - optional parameter for uploading in lieu of given player, if user have permission to do so. Useful for console.  
`Name` - ID of the map. Should be unique per user. Is used for getting painting again with command `/getpainting` if needed.  
`URL` - url of image to upload.  
`MapsX`, `MapsY` - image size in maps. `1 1` by default. For example, `3 2` means painting will be occupy 3x2 blocks when placed in a world.  
`ScaleMode` - scale mode. Lanczos3 by default. Use Mitchell if Lanczos3 gives artifacts. Use NoScale to place image to center without scaling. Possible values: NoScale, BSpline, Bell, BiCubic, BiCubicHighFreqResponse, BoxFilter, Hermite, Lanczos3, Mitchell, Triangle.  
`DitherMode` - dither mode. FloydSteinberg by default. More about that below. Possible values: NoDither, FloydSteinberg, JarvisJudiceNinke, Stucki, Atkinson, Burkes, Sierra3, TwoRowSierra, SierraLite, OrderedBayer2x2, OrderedBayer4x4, OrderedBayer8x8, OrderedBayer16x16.  
`ColorBleedReductionPercent` - applicable only to unordered dither. Possible values: 0-100. Zero percent by default. More about that below.  
*Examples:*  
`/uploadpainting Lenna1 https://upload.wikimedia.org/wikipedia/en/2/24/Lenna.png`  
`/uploadpainting Lenna2 https://upload.wikimedia.org/wikipedia/en/2/24/Lenna.png 4 4`  
`/uploadpainting Lenna3 https://upload.wikimedia.org/wikipedia/en/2/24/Lenna.png 4 4 Mitchell NoDither`

`/getpainting [Player] <Name>` - Gets painting by given name. If user have permission, user can specify owner player.  
Player - optional parameter for getting paiting owned of given player, if user have permission to do so.  
Name - name of a painting that was specified in `/uploadpainting` command.  
*Examples:*  
`/getpainting BeautifulSunset`  
`/getpainting Notch MobPoker`

`/paintingslist [all|Player] [page]` - Displays a list of paintings and info about them. You can click ">GET<" links to get paintings.  
`all` - specify this parameter for viewing a list of all paintings of all players, if you have permission to do so.  
`Player` - specify this parameter for viewing a list of paintings of given player, if you have permission to do so.  
If neither of those parameters are present, a list of your own paintings will be displayed.  
`page` - page  
*Examples:*  
`/paintingslist`  
`/paintingslist 2`  
`/paintingslist all`  
`/paintingslist Player123`

**Dither**  
Maps have limited palette in Minecraft, so it can't recreate all the colors, especially in gradients. To mitigate this, you can use [dither](https://en.wikipedia.org/wiki/Dither). Disther adds noise to simulate colors in between palette values. There are two types of dither: unordered and ordered. Supported unordered dither: FloydSteinberg, JarvisJudiceNinke, Stucki, Atkinson, Burkes, Sierra3, TwoRowSierra, SierraLite. Supported ordered dither: OrderedBayer2x2, OrderedBayer4x4, OrderedBayer8x8, OrderedBayer16x16. Please note that OrderedBayer algorithm used here is not intended for images with indexed palette, so it won't work as effectively.

Comparison ([original](https://upload.wikimedia.org/wikipedia/en/2/24/Lenna.png)):  
No dither (NoDither):  
![img](https://orig12.deviantart.net/f7e5/f/2017/228/7/5/nodither_by_vcsajen-dbk8xex.png)

Ordered dither (OrderedBayer16x16):  
![img](https://orig08.deviantart.net/1f62/f/2017/228/0/c/ordereddither_by_vcsajen-dbk8xf4.png)

Unordered dither (FloydSteinberg):  
![img](https://orig01.deviantart.net/a227/f/2017/228/1/2/unordereddither_by_vcsajen-dbk8xf2.png)

ColorBleedReductionPercent parameter is intended for reduction of "color bleed" (unordered dither sometimes causes color to "bleed" below and left) and lead to increasing areas of solid color. Only for unordered dither.

Color bleed reduction at 75% (Atkinson):  
![img](https://orig05.deviantart.net/1fd5/f/2017/228/2/0/colorbleedreduction_by_vcsajen-dbk8y1z.png)

Sometimes dither isn't needed and image would look better without it, primarily where only bright primary colors were used, or when image have lots of gray colors.

**Painting placer**  
When user executes command `/uploadpainting` or command `/getpainting` user will receive: filled map, if painting is 1x1 size, just place it in item frame; or painting placer for bigger paintings. Painting placer is just a stick; click with it any wall with sufficient empty space to place a painting. Painting will automatically fit to any available space, but if you need to place it more precisely, just know that click point will become upper left corner of a painting, if space allows.

**Permissions**  
List of available permissions and description of what they do:  
`yourcustompaintings.commands.uploadpainting.execute` - Allows the user to execute the `/uploadpainting` command.  
`yourcustompaintings.commands.uploadpainting.others` - Allows the Operator to upload painting under guise of another user account  
`yourcustompaintings.commands.getpainting.execute` - Allows the user to execute the uploadpainting command.  
`yourcustompaintings.commands.getpainting.others` - Allows the user to get paintings of other users with /getpainting  
`yourcustompaintings.commands.paintingslist.execute` - Allows the user to execute the paintingslist command.  
`yourcustompaintings.commands.paintingslist.all` - Allows the user to get list of all paintings  
`yourcustompaintings.commands.paintingslist.others` - Allows the user to get list of paintings of other users  
`yourcustompaintings.paintingplacer.place` - Allows the user place paintings with painting placer. Usually admins allow all users to have it by default  
`yourcustompaintings.paintingplacer.dontselfdestruct` - Allows the user to place paintings with painting placer any times they want in Survival. Please note that even if they don't have this permission, they can still copy individual maps that's contains painting with vanilla method. Users in Creative don't need this permission.  
`yourcustompaintings.bypasssurvival` - Users who have this permission do not need any items for getting/placing paintings in Survival. Users who don't have this permission will need to have Empty Map to get 1x1 painting, Stick to get Painting Placer, Empty Maps and Item Frames in quantity of MapsX*MapsY each for placing a painting with painting placer.

**Permission Options**  
List of available permission options and description of what they do:  
`yourcustompaintings.commands.uploadpainting.max.maps.x` - max allowed width of a painting for player in blocks/maps  
`yourcustompaintings.commands.uploadpainting.max.maps.y` - max allowed height of a painting for player in blocks/maps  
`yourcustompaintings.commands.uploadpainting.max.imgfilesize` - max allowed size of image to upload for player in bytes  
`yourcustompaintings.commands.uploadpainting.max.imgwidth` - max allowed width of image to upload for player in pixels  
`yourcustompaintings.commands.uploadpainting.max.imgheight` - max allowed height of image to upload for player in pixels

**Config**  
`debug-mode` - Debug mode. Will write some trash info in plugin's config folder, so not recommended. `false` by default.  
`max-img-file-size` - Maximal size of image file to upload in bytes. 1048576 by default.  
`progress-report-time` - Report progress of operation where possible every X milliseconds (currently only upload progress). 30000 by default.

**Limitations**  
Uploading image with too big dimensions, or too big in-game size in blocks will cause OutOfMemoryError, so make sure to adjust limits according to your server capacity. Minecraft support only ~32000 maps, so be sure to not upload too much, and only give permission to players who have a sense of limit. If you upload too much, regular maps and uploaded paintings will start overwriting each other, which is not pretty. There's no guarantee that future versions of Minecraft/Sponge will like those paintings-maps, so I take no responsibility for anything.

**Screenshots**  
[![img](https://t02.deviantart.net/MwvSFJRqJ1fnTs6UsfwqhtjtcXo=/fit-in/150x150/filters:no_upscale():origin()/pre11/d733/th/pre/i/2017/229/0/3/2017_08_17_19_54_53_by_vcsajen-dbkcsft.png)](https://orig11.deviantart.net/8ca9/f/2017/229/4/d/2017_08_17_19_54_53_by_vcsajen-dbkcsft.png) [![img](https://t04.deviantart.net/_OvT8kC0fpgE-gsbAT6dqkntRmo=/fit-in/150x150/filters:no_upscale():origin()/pre12/eddb/th/pre/i/2017/229/5/0/2017_08_17_20_00_50_by_vcsajen-dbkcsfp.png)](https://orig14.deviantart.net/3bdf/f/2017/229/c/6/2017_08_17_20_00_50_by_vcsajen-dbkcsfp.png) [![img](https://t07.deviantart.net/1y2ONJPLcvBUrmk0-h3gg2c8EJo=/fit-in/150x150/filters:no_upscale():origin()/pre00/76e0/th/pre/i/2017/229/3/d/2017_08_17_19_03_22_by_vcsajen-dbkcsfm.png)](https://orig12.deviantart.net/e1d0/f/2017/229/8/8/2017_08_17_19_03_22_by_vcsajen-dbkcsfm.png)

**Quick video demonstration**  
https://youtu.be/wIjFeIylXjs

**Resources used in screenshots and video**  
[“Fennec Fox”](https://www.flickr.com/photos/keithroper/5185867854) by _[Keith Roper](https://www.flickr.com/people/keithroper/)_ is licensed under [CC BY 2.0](https://creativecommons.org/licenses/by/2.0)  
[“The End of Words”](https://busangane.deviantart.com/art/The-End-of-Words-302148926) by _[busangane](https://busangane.deviantart.com/)_ is licensed under [CC BY SA 3.0](https://creativecommons.org/licenses/by-sa/3.0)  
[“bridge”](https://busangane.deviantart.com/art/bridge-303723558) by _[busangane](https://busangane.deviantart.com/)_ is licensed under [CC BY 3.0](https://creativecommons.org/licenses/by/3.0)  
[“Adira Portrait”](https://twokinds.deviantart.com/art/Adira-Portrait-535796618) by _[Twokinds](https://twokinds.deviantart.com/)_ is licensed under [CC BY SA 3.0](http://creativecommons.org/licenses/by-nc-sa/3.0/us/)  
[“DSC_0794”](https://www.flickr.com/photos/rachidh/35197680502/) by _[Rachid H](https://www.flickr.com/photos/rachidh/)_ is licensed under [CC BY NC 2.0](https://creativecommons.org/licenses/by-nc/2.0/)  
[“Epic KItty Battle”](https://www.flickr.com/photos/farlane/32918665514/) by _[Andrew McFarlane](https://www.flickr.com/photos/farlane/)_ is licensed under [CC BY NC 2.0](https://creativecommons.org/licenses/by-nc/2.0/)

  
**Sponge Version**  
Tested on:  
- SpongeForge 1.12.2-2705-7.1.0-BETA-3440
- SpongeVanilla 1.12.2-7.1.0-BETA-116


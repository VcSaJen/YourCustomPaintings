package com.vcsajen.yourcustompaintings;

import com.flowpowered.math.imaginary.Quaterniond;
import com.flowpowered.math.matrix.Matrix3d;
import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.Lists;
import com.vcsajen.yourcustompaintings.database.PaintingRecord;
import com.vcsajen.yourcustompaintings.util.BestFitRect;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.property.BooleanProperty;
import org.spongepowered.api.data.property.block.PassableProperty;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.hanging.ItemFrame;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.util.Identifiable;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by VcSaJen on 12.08.2017 11:07.
 */
public class PaintingPlacer {
    private YourCustomPaintings mainClass;
    private String itemName = "Painting Placer";

    public static ItemStack getPaintingMapItem(int id) {
        ItemStack itemStack = ItemStack.builder().itemType(ItemTypes.FILLED_MAP).quantity(1).build();
        DataView rawData = itemStack.toContainer();
        rawData.set(DataQuery.of("UnsafeDamage"), id);
        rawData.set(DataQuery.of("UnsafeData", "display", "LocName"), "item.painting.name");
        rawData.set(DataQuery.of("UnsafeData", "display", "MapColor"), 16744576);

        itemStack = ItemStack.builder().fromContainer(rawData).build();
        return itemStack;
    }

    public ItemStack getPlacerItem(PaintingRecord paintingRecord)
    {

        return ItemStack.builder()
                .itemType(ItemTypes.STICK)
                .quantity(1)
                .keyValue(Keys.DISPLAY_NAME, Text.of(TextStyles.RESET, itemName))
                .keyValue(Keys.ITEM_LORE, Lists.newArrayList(
                        Text.of("\""+paintingRecord.getName()+"\""),
                        Text.of("by "+mainClass.userStorageService.get(paintingRecord.getOwner()).map(User::getName).orElse("%UUID%"+paintingRecord.getOwner().toString()+"%") ),
                        Text.of("#"+paintingRecord.getStartMapId()+" "+paintingRecord.getMapsX()+"x"+paintingRecord.getMapsY())
                ))
                .build();
    }

    private Pattern pname = Pattern.compile("^\"(.+)\"$");
    private Pattern puseruuid = Pattern.compile("^by %UUID%([-0-9A-Fa-f]+)%$");
    private Pattern pusername = Pattern.compile("^by ([^%]+)$");
    private Pattern psomethings = Pattern.compile("^#(\\d+) (\\d+)x(\\d+)$");

    @Nullable
    private PaintingRecord getPaintingFromPlacerItem(ItemStack item) {
        List<Text> textList = item.get(Keys.ITEM_LORE).orElse(null);
        if (textList == null || textList.size()!=3) return null;

        Matcher mname = pname.matcher(textList.get(0).toPlain());
        Matcher museruuid = puseruuid.matcher(textList.get(1).toPlain());
        Matcher musername = pusername .matcher(textList.get(1).toPlain());
        Matcher msomethings = psomethings.matcher(textList.get(2).toPlain());
        String name;
        UUID useruuid;
        int mapsX, mapsY, startMapId;
        if (!(mname.matches() && (museruuid.matches() || musername.matches()) && msomethings.matches()))
            return null;
        try {
            name = mname.group(1);
            if (musername.matches()) {
                useruuid = mainClass.userStorageService.get(musername.group(1)).map(Identifiable::getUniqueId).orElse(null);
                if (useruuid==null) return null;
            } else useruuid = UUID.fromString(museruuid.group(1));
            mapsX = Integer.parseInt(msomethings.group(2));
            mapsY = Integer.parseInt(msomethings.group(3));
            startMapId = Integer.parseInt(msomethings.group(1));
        } catch (IllegalArgumentException e) {
            return null;
        }

        return new PaintingRecord(useruuid, name, mapsX, mapsY, startMapId);
    }

    private boolean isPlacerItem(ItemStack item) {
        return item!=null && ItemTypes.STICK.matches(item) && item.get(Keys.DISPLAY_NAME).orElse(Text.of()).toPlain().equals(itemName);
    }

    private boolean placePaintingIfPossible(PaintingRecord paintingRecord, Location<World> loc, Direction dir) {
        World world = loc.getExtent(); //local variable, so it's fine
        Vector3i v = loc.getBlockPosition();
        boolean isPassable = loc.getProperty(PassableProperty.class).map(BooleanProperty::getValue).orElse(true);
        if (isPassable) return false;
        int pw=paintingRecord.getMapsX();
        int ph=paintingRecord.getMapsY();
        Vector3i leftDirection = Matrix3d.createRotation(Quaterniond.fromAxesAnglesDeg(0,90,0)).transform(dir.asOffset()).round().toInt();
        Vector3i intoDirection = dir.asBlockOffset().negate();

        Vector3i bigRectStart = v.sub(leftDirection.mul(pw-1)).sub(0, -(ph-1), 0);
        int bigRectW = pw*2-1;
        int bigRectH = ph*2-1;
        boolean[][] matrix = new boolean[bigRectW][bigRectH];
        for (int i = 0; i <= bigRectW-1; i++) {
            for (int j = 0; j <= bigRectH-1; j++) {
                Vector3i v1 = bigRectStart.add(leftDirection.mul(i)).add(0, -j, 0);
                if (v1.getY()<0 || v1.getY()>255) {
                    matrix[i][j] = false;
                    continue;
                }

                boolean b1 = (new Location<World>(world, v1)).getProperty(PassableProperty.class).map(BooleanProperty::getValue).orElse(true);

                Vector3i v2 = v1.add(dir.asBlockOffset());
                Location<World> loc2 = new Location<World>(world, v2);
                boolean b2 = loc2.getBlockType().equals(BlockTypes.AIR);

                Chunk curChuck = world.getChunkAtBlock(v2).orElse(null);
                if (curChuck == null) return false;
                boolean b3 = curChuck.getEntities(entity ->
                        entity.getType().equals(EntityTypes.ITEM_FRAME) &&
                        entity.getTransform().getPosition().floor().toInt().equals(v2) &&
                        entity.get(Keys.DIRECTION).get().equals(dir)).isEmpty();

                matrix[i][j] = !b1 && b2 && b3;
            }
        }
        BestFitRect.Rectangle rect = BestFitRect.getBestFitRect(matrix);
        if (rect == null) return false;
        Vector2i offset2d = rect.getOffset();
        Vector3i offset = (new Vector3i(0,0,0)).add(leftDirection.mul(offset2d.getX())).add(0, -offset2d.getY(), 0);
        for (int i = 0; i < pw; i++) {
            for (int j = 0; j < ph; j++) {
                Vector3d placePlace = v.add(offset).add(dir.asBlockOffset()).add(leftDirection.mul(i)).add(0, -j, 0).toDouble();
                Entity itemFrame = world
                        .createEntity(EntityTypes.ITEM_FRAME, placePlace);
                ((ItemFrame)itemFrame).setLocation(new Location<>(world, placePlace));
                itemFrame.tryOffer(Keys.REPRESENTED_ITEM, getPaintingMapItem(paintingRecord.getStartMapId()+j*pw+i).createSnapshot());
                itemFrame.tryOffer(Keys.DIRECTION, dir);
                world.spawnEntity(itemFrame);
            }
        }

        return true;
    }

    boolean checkPlayersItems(Player player, PaintingRecord paintingRecord)
    {
        //if (player.getInventory().query(ItemStack.builder().build())) ;
        return false;
    }

    @Listener
    public void onInteractBlockEvent(InteractBlockEvent.Secondary.MainHand event, @Root Player eventSrc) {
        if (event.getTargetBlock().getLocation().isPresent() && eventSrc.hasPermission("yourcustompaintings.paintingplacer.place")) {
            ItemStack item = eventSrc.getItemInHand(HandTypes.MAIN_HAND).orElse(null);
            if (isPlacerItem(item)) {
                PaintingRecord paintingRecord = getPaintingFromPlacerItem(item);
                if (paintingRecord!=null) {
                    boolean hasRequiredItems;
                    boolean pollPending = false;
                    Inventory invFrames = null;
                    Inventory invEmptyMaps = null;
                    if (!eventSrc.hasPermission("yourcustompaintings.bypasssurvival") && eventSrc.gameMode().get() != GameModes.CREATIVE) {
                        invFrames = eventSrc.getInventory().query(ItemTypes.ITEM_FRAME);
                        invEmptyMaps = eventSrc.getInventory().query(ItemTypes.MAP);
                        if (invFrames.totalItems()>=paintingRecord.getLengthMapId() && invEmptyMaps.totalItems()>=paintingRecord.getLengthMapId()) {
                            hasRequiredItems = true;
                            pollPending = true;
                        } else {
                            hasRequiredItems = false;
                            eventSrc.sendMessage(Text.of(TextColors.RED, "Not enough items! " +
                                    String.format("Required: %1$dx%2$s, %1$dx%3$s. Found: %4$dx%2$s, %5$dx%3$s.",
                                            paintingRecord.getLengthMapId(),
                                            ItemTypes.ITEM_FRAME.getName(),
                                            ItemTypes.MAP.getName(),
                                            invFrames.totalItems(),
                                            invEmptyMaps.totalItems())));
                        }
                    } else hasRequiredItems = true;

                    if (hasRequiredItems) {
                        boolean placed = placePaintingIfPossible(paintingRecord, event.getTargetBlock().getLocation().get(), event.getTargetSide());
                        if (placed) {
                            if (pollPending) {
                                invFrames.poll(paintingRecord.getLengthMapId());
                                invEmptyMaps.poll(paintingRecord.getLengthMapId());
                            }
                            if (!eventSrc.hasPermission("yourcustompaintings.paintingplacer.dontselfdestruct") && eventSrc.gameMode().get() != GameModes.CREATIVE) {
                                item.setQuantity(item.getQuantity() - 1);
                                if (item.getQuantity() == 1)
                                    eventSrc.setItemInHand(HandTypes.MAIN_HAND, null);
                                else eventSrc.setItemInHand(HandTypes.MAIN_HAND, item);
                            }

                        }
                    }
                }
            }
        }

    }

    public PaintingPlacer(YourCustomPaintings main){
        mainClass = main;
    }
}

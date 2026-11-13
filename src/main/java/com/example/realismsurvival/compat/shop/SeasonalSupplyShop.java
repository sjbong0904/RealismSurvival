package com.example.realismsurvival.compat.shop;

import com.example.realismsurvival.RealismSurvival;
import com.example.realismsurvival.RealismSurvivalConfig;
import com.example.realismsurvival.environment.ClimateZone;
import com.example.realismsurvival.environment.Season;
import com.example.realismsurvival.environment.SeasonCalendar;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Rebuilds one Q Shop per climate and merchant category when the overworld season or shop era changes.
 * Lists live in {@code config/realismsurvival/supply_shop_seasons.json}.
 * A climate with its own {@code seasons} replaces the shared seasonal list; its {@code always} is added on top.
 * Each stall sells only its category. Every stall buys any ordinary stack, paying more for its own goods.
 * Player shops are RS chest GUIs (hub → buy catalog or sell counter). The season JSON still rebuilds
 * Q Shop files as a catalog mirror when that mod is present. Buyback goes through {@link SupplyCounterTrade}.
 */
@EventBusSubscriber(modid = RealismSurvival.MODID)
public final class SeasonalSupplyShop {

    private static final Path CONFIG_PATH =
            FMLPaths.CONFIGDIR.get().resolve("realismsurvival").resolve("supply_shop_seasons.json");
    private static final int CHECK_INTERVAL = 200;

    private static Catalog catalog = Catalog.defaults();
    private static Season lastApplied;
    private static SupplyEra lastEra;
    private static boolean lastCounter;
    private static long configLoadedAt;

    private SeasonalSupplyShop() {}

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        reloadCatalog();
        if (!ShopMods.qshop()) {
            return;
        }
        ServerLevel overworld = event.getServer().overworld();
        apply(SeasonCalendar.from(overworld).season(), SupplyEra.fromLevel(overworld), true);
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) {
            return;
        }
        if (level.getGameTime() % CHECK_INTERVAL != 0L) {
            return;
        }
        maybeReloadCatalogFromDisk();
        if (!ShopMods.qshop()) {
            return;
        }
        Season season = SeasonCalendar.from(level).season();
        SupplyEra era = SupplyEra.fromLevel(level);
        boolean counter = counterEnabled();
        if (season != lastApplied || era != lastEra || counter != lastCounter) {
            apply(season, era, counter != lastCounter);
        }
    }

    static String climateName(ClimateZone zone) {
        return Catalog.climateLabel(zone);
    }

    static boolean counterEnabled() {
        return RealismSurvivalConfig.SUPPLY_COUNTER_ENABLED.getAsBoolean();
    }

    /** Player-facing buy lines for one merchant (shop sells these). */
    public static List<BuyOffer> buyOffers(ServerLevel level, ClimateZone zone, SupplyCategory category) {
        ServerLevel overworld = level.getServer().overworld();
        Season season = SeasonCalendar.from(overworld).season();
        SupplyEra era = SupplyEra.fromLevel(overworld);
        List<BuyOffer> offers = new ArrayList<>();
        for (Offer offer : merchantOffers(season, zone, category, era)) {
            if (offer.buy()) {
                offers.add(new BuyOffer(
                        offer.itemId(),
                        Math.max(1, offer.count()),
                        Math.max(0, (int) Math.round(offer.price())),
                        Math.max(1, offer.playerLimit())));
            }
        }
        return offers;
    }

    /** Current buyback rates of one merchant, keyed by item id. Same lines Q Shop would list as SELL. */
    static Map<String, SupplyPricePolicy.Rate> buybackRates(ServerLevel level, ClimateZone zone, SupplyCategory category) {
        ServerLevel overworld = level.getServer().overworld();
        Season season = SeasonCalendar.from(overworld).season();
        SupplyEra era = SupplyEra.fromLevel(overworld);
        Map<String, SupplyPricePolicy.Rate> rates = new HashMap<>();
        for (Offer offer : merchantOffers(season, zone, category, era)) {
            if (!offer.buy()) {
                rates.putIfAbsent(
                        offer.itemId().toString(),
                        new SupplyPricePolicy.Rate(
                                Math.max(1, offer.count()), (int) Math.round(offer.price()), Math.max(1, offer.playerLimit())));
            }
        }
        return rates;
    }

    public static void reloadCatalog() {
        ensureDefaultConfig();
        catalog = Catalog.load(CONFIG_PATH);
        configLoadedAt = CONFIG_PATH.toFile().lastModified();
        RealismSurvival.LOGGER.info(
                "[RealismSurvival] Loaded seasonal supply catalog ({} seasons)", catalog.seasonOffers().size());
    }

    private static void maybeReloadCatalogFromDisk() {
        long modified = CONFIG_PATH.toFile().lastModified();
        if (modified > 0L && modified != configLoadedAt) {
            catalog = Catalog.load(CONFIG_PATH);
            configLoadedAt = modified;
            lastApplied = null;
            lastEra = null;
        }
    }

    static String shopId(ClimateZone zone, SupplyCategory category) {
        String climate = switch (zone) {
            case ARID -> "arid";
            case COLD -> "cold";
            case TROPICAL -> "tropical";
            default -> "temperate";
        };
        return "supply_post_" + climate + "_" + category.id();
    }

    private static void apply(Season season, SupplyEra era, boolean force) {
        if (!force && season == lastApplied && era == lastEra) {
            return;
        }
        boolean counter = counterEnabled();
        for (ClimateZone zone : ClimateZone.values()) {
            if (zone == ClimateZone.OCEAN) {
                continue;
            }
            for (SupplyCategory category : SupplyCategory.values()) {
                try {
                    rebuildShop(season, zone, category, era, counter);
                } catch (ReflectiveOperationException | IOException | RuntimeException ex) {
                    RealismSurvival.LOGGER.warn(
                            "[RealismSurvival] Failed to apply supply shop {} {} {}: {}",
                            zone,
                            category.id(),
                            season.getSerializedName(),
                            ex.toString());
                }
            }
        }
        lastApplied = season;
        lastEra = era;
        lastCounter = counter;
        RealismSurvival.LOGGER.info(
                "[RealismSurvival] Supply shops set to season {} era {} (sell counter {})",
                season.getSerializedName(),
                era.label(),
                counter ? "on" : "off");
    }

    private static void rebuildShop(
            Season season, ClimateZone zone, SupplyCategory category, SupplyEra era, boolean buyOnly)
            throws ReflectiveOperationException, IOException {
        Class<?> manager = Class.forName("com.qshop.shop.ShopManager");
        Class<?> shopClass = Class.forName("com.qshop.shop.Shop");
        Class<?> entryClass = Class.forName("com.qshop.shop.ShopEntry");
        Class<?> entryTypeClass = Class.forName("com.qshop.shop.ShopEntryType");
        Class<?> limitResetClass = Class.forName("com.qshop.shop.LimitReset");
        Class<?> tabClass = Class.forName("com.qshop.shop.ShopTab");

        String shopId = shopId(zone, category);
        String title = catalog.titleFor(season, zone) + " · " + category.label() + " · " + era.label();
        Method get = manager.getMethod("get", String.class);
        Method save = manager.getMethod("save", shopClass);
        Object shop = get.invoke(null, shopId);
        if (shop == null) {
            shop = shopClass.getConstructor().newInstance();
            shopClass.getField("id").set(shop, shopId);
        }

        shopClass.getField("displayName").set(shop, title);
        shopClass.getField("currency").set(shop, catalog.currencyId());
        shopClass.getMethod("ensureTabs").invoke(shop);

        @SuppressWarnings("unchecked")
        List<Object> rootEntries = (List<Object>) shopClass.getField("entries").get(shop);
        rootEntries.clear();

        Object tab = shopClass.getMethod("tab", int.class).invoke(shop, 0);
        @SuppressWarnings("unchecked")
        List<Object> tabEntries = (List<Object>) tabClass.getField("entries").get(tab);
        tabEntries.clear();
        tabClass.getField("name").set(tab, title);

        @SuppressWarnings({"unchecked", "rawtypes"})
        Object buyType = Enum.valueOf((Class) entryTypeClass, "BUY");
        @SuppressWarnings({"unchecked", "rawtypes"})
        Object sellType = Enum.valueOf((Class) entryTypeClass, "SELL");
        @SuppressWarnings({"unchecked", "rawtypes"})
        Object daily = Enum.valueOf((Class) limitResetClass, "DAILY");

        List<Offer> offers = merchantOffers(season, zone, category, era);
        for (Offer offer : offers) {
            if (buyOnly && !offer.buy()) {
                continue;
            }
            ItemStack stack = offer.toStack();
            if (stack.isEmpty()) {
                continue;
            }
            Object entry = entryClass.getConstructor().newInstance();
            entryClass.getField("type").set(entry, offer.buy() ? buyType : sellType);
            entryClass.getField("displayName").set(entry, "");
            entryClass.getField("description").set(entry, "");
            entryClass.getField("displayItem").set(entry, stack.copy());
            entryClass.getField("item").set(entry, stack.copy());
            entryClass.getField("currencyId").set(entry, catalog.currencyId());
            entryClass.getField("price").set(entry, offer.price());
            entryClass.getField("playerLimit").set(entry, offer.playerLimit());
            entryClass.getField("globalLimit").set(entry, 0);
            entryClass.getField("reset").set(entry, daily);

            @SuppressWarnings("unchecked")
            List<ItemStack> give = (List<ItemStack>) entryClass.getField("give").get(entry);
            @SuppressWarnings("unchecked")
            List<ItemStack> receive = (List<ItemStack>) entryClass.getField("receive").get(entry);
            give.clear();
            receive.clear();
            if (offer.buy()) {
                give.add(stack.copy());
            } else {
                receive.add(stack.copy());
            }
            entryClass.getMethod("ensureUuid").invoke(entry);
            tabEntries.add(entry);
        }

        // Keep root list empty; Q Shop uses tabs after ensureTabs.
        Field dataVersion = shopClass.getField("dataVersion");
        dataVersion.setInt(shop, Math.max(1, dataVersion.getInt(shop)));
        save.invoke(null, shop);
    }

    private static List<Offer> merchantOffers(Season season, ClimateZone zone, SupplyCategory category, SupplyEra era) {
        List<SupplyStock.Line> raw = new ArrayList<>();
        for (Offer offer : catalog.offersFor(season, zone)) {
            raw.add(new SupplyStock.Line(
                    offer.buy(), offer.itemId().toString(), offer.count(), (int) Math.round(offer.price()), offer.playerLimit()));
        }
        List<Offer> out = new ArrayList<>();
        for (SupplyStock.Line line : SupplyStock.forMerchant(raw, zone, season, category, era)) {
            out.add(new Offer(line.buy(), Identifier.parse(line.item()), line.count(), line.price(), line.playerLimit()));
        }
        return out;
    }

    private static void ensureDefaultConfig() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                return;
            }
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, Catalog.defaultJson(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            RealismSurvival.LOGGER.warn("[RealismSurvival] Could not write default seasonal shop config: {}", ex.toString());
        }
    }

    public record BuyOffer(Identifier itemId, int count, int price, int playerLimit) {
        public int dailyUnits() {
            return count * playerLimit;
        }
    }

    private record Offer(boolean buy, Identifier itemId, int count, double price, int playerLimit) {
        ItemStack toStack() {
            Item item = BuiltInRegistries.ITEM.getValue(itemId);
            if (item == null || item == Items.AIR) {
                RealismSurvival.LOGGER.warn("[RealismSurvival] Unknown shop item {}", itemId);
                return ItemStack.EMPTY;
            }
            return new ItemStack(item, Math.max(1, count));
        }
    }

    private record ClimateStock(List<Offer> always, Map<Season, List<Offer>> seasons, boolean replacesSeasons) {}

    private record Catalog(
            String currency,
            Map<Season, String> titles,
            Map<Season, List<Offer>> bySeason,
            List<Offer> always,
            Map<ClimateZone, ClimateStock> climates) {

        Map<Season, List<Offer>> seasonOffers() {
            return bySeason;
        }

        String currencyId() {
            return currency == null || currency.isBlank() ? ShopMods.LINKED_CURRENCY : currency;
        }

        String titleFor(Season season, ClimateZone zone) {
            String base = titles.getOrDefault(season, "계절 공급소 · " + season.displayName().getString());
            return base + " · " + climateLabel(zone);
        }

        List<Offer> offersFor(Season season, ClimateZone zone) {
            ClimateZone key = zone == ClimateZone.OCEAN ? ClimateZone.TEMPERATE : zone;
            List<Offer> out = new ArrayList<>(always);
            ClimateStock local = climates.get(key);
            if (local != null) {
                out.addAll(local.always());
            }
            if (local != null && local.replacesSeasons()) {
                out.addAll(local.seasons().getOrDefault(season, List.of()));
            } else {
                out.addAll(bySeason.getOrDefault(season, List.of()));
            }
            return out;
        }

        private static String climateLabel(ClimateZone zone) {
            return switch (zone) {
                case ARID -> "건조";
                case COLD -> "한대";
                case TROPICAL -> "열대";
                case OCEAN -> "해안";
                case TEMPERATE -> "온대";
            };
        }

        static Catalog defaults() {
            return loadFromJson(JsonParser.parseString(defaultJson()).getAsJsonObject());
        }

        static Catalog load(Path path) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                return loadFromJson(JsonParser.parseReader(reader).getAsJsonObject());
            } catch (Exception ex) {
                RealismSurvival.LOGGER.warn(
                        "[RealismSurvival] Bad seasonal shop config {}, using defaults: {}", path, ex.toString());
                return defaults();
            }
        }

        static Catalog loadFromJson(JsonObject root) {
            String currency = root.has("currency") ? root.get("currency").getAsString() : ShopMods.LINKED_CURRENCY;
            Map<Season, String> titles = new EnumMap<>(Season.class);
            if (root.has("displayNameBySeason") && root.get("displayNameBySeason").isJsonObject()) {
                JsonObject names = root.getAsJsonObject("displayNameBySeason");
                for (Season season : Season.values()) {
                    String key = season.getSerializedName();
                    if (names.has(key)) {
                        titles.put(season, names.get(key).getAsString());
                    }
                }
            }
            List<Offer> always = readSide(root.getAsJsonObject("always"));
            Map<Season, List<Offer>> bySeason = new EnumMap<>(Season.class);
            JsonObject seasons = root.has("seasons") ? root.getAsJsonObject("seasons") : new JsonObject();
            for (Season season : Season.values()) {
                JsonObject node = seasons.has(season.getSerializedName())
                        ? seasons.getAsJsonObject(season.getSerializedName())
                        : new JsonObject();
                bySeason.put(season, readSide(node));
            }
            Map<ClimateZone, ClimateStock> climates = new EnumMap<>(ClimateZone.class);
            if (root.has("climates") && root.get("climates").isJsonObject()) {
                JsonObject all = root.getAsJsonObject("climates");
                for (ClimateZone zone : ClimateZone.values()) {
                    String key = zone.name().toLowerCase(Locale.ROOT);
                    if (!all.has(key) || !all.get(key).isJsonObject()) {
                        continue;
                    }
                    JsonObject node = all.getAsJsonObject(key);
                    List<Offer> extra = readSide(node.has("always") ? node.getAsJsonObject("always") : null);
                    Map<Season, List<Offer>> localSeasons = new EnumMap<>(Season.class);
                    boolean replaces = node.has("seasons") && node.get("seasons").isJsonObject();
                    if (replaces) {
                        JsonObject local = node.getAsJsonObject("seasons");
                        for (Season season : Season.values()) {
                            String seasonKey = season.getSerializedName();
                            JsonObject seasonNode = local.has(seasonKey) && local.get(seasonKey).isJsonObject()
                                    ? local.getAsJsonObject(seasonKey)
                                    : null;
                            localSeasons.put(season, readSide(seasonNode));
                        }
                    }
                    climates.put(zone, new ClimateStock(extra, localSeasons, replaces));
                }
            }
            return new Catalog(currency, titles, bySeason, always, climates);
        }

        private static List<Offer> readSide(JsonObject node) {
            if (node == null) {
                return List.of();
            }
            List<Offer> out = new ArrayList<>();
            out.addAll(readOffers(node.getAsJsonArray("buy"), true));
            out.addAll(readOffers(node.getAsJsonArray("sell"), false));
            return List.copyOf(out);
        }

        private static List<Offer> readOffers(JsonArray array, boolean buy) {
            if (array == null) {
                return List.of();
            }
            List<Offer> out = new ArrayList<>();
            for (JsonElement element : array) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject o = element.getAsJsonObject();
                if (!o.has("item")) {
                    continue;
                }
                Identifier id = Identifier.parse(o.get("item").getAsString());
                int count = o.has("count") ? o.get("count").getAsInt() : 1;
                double price = o.has("price") ? o.get("price").getAsDouble() : 1.0;
                int limit = o.has("playerLimit") ? o.get("playerLimit").getAsInt() : 4;
                out.add(new Offer(buy, id, count, price, limit));
            }
            return out;
        }

        static String defaultJson() {
            try (InputStream in = SeasonalSupplyShop.class.getResourceAsStream("/supply_shop_seasons.default.json")) {
                if (in == null) {
                    throw new IOException("missing default supply catalog");
                }
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException ex) {
                RealismSurvival.LOGGER.warn("[RealismSurvival] Could not read default supply catalog: {}", ex.toString());
                return "{\"currency\":\"coins\"}";
            }
        }
    }
}

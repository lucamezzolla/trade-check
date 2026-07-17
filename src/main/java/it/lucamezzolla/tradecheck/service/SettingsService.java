package it.lucamezzolla.tradecheck.service;

import it.lucamezzolla.tradecheck.model.BrokerProfile;
import it.lucamezzolla.tradecheck.model.CommissionBasis;
import it.lucamezzolla.tradecheck.model.MaximumType;
import it.lucamezzolla.tradecheck.model.TradeCurrency;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public final class SettingsService {
    private static final Preferences PREFS = Preferences.userNodeForPackage(SettingsService.class);
    private static final String PROFILE_IDS_KEY = "profiles.ids";
    private static final String ACTIVE_PROFILE_KEY = "profiles.active";
    private static final String PROFILE_NODE = "profiles";
    private static final String SCHEMA_VERSION_KEY = "profiles.schema.version";
    private static final int SCHEMA_VERSION = 2;

    public static final String FINECO_ITALY_ID = "fineco-italy";
    public static final String IBKR_EU_TIERED_ID = "ibkr-europe-tiered";
    public static final String IBKR_EU_FIXED_ID = "ibkr-europe-fixed";
    public static final String IBKR_US_TIERED_ID = "ibkr-usa-tiered";
    public static final String IBKR_US_FIXED_ID = "ibkr-usa-fixed";

    private static final String LEGACY_FINECO_ID = "fineco";
    private static final String LEGACY_IBKR_ID = "interactive-brokers";

    public List<BrokerProfile> loadProfiles() {
        ensureProfiles();
        List<BrokerProfile> profiles = new ArrayList<>();
        for (String id : loadProfileIds()) {
            BrokerProfile profile = loadProfile(id);
            if (profile != null) {
                profiles.add(profile);
            }
        }
        return profiles;
    }

    public BrokerProfile loadActiveProfile() {
        List<BrokerProfile> profiles = loadProfiles();
        if (profiles.isEmpty()) {
            throw new IllegalStateException("No broker profiles available");
        }

        String activeId = PREFS.get(ACTIVE_PROFILE_KEY, profiles.getFirst().id());
        return profiles.stream()
                .filter(profile -> profile.id().equals(activeId))
                .findFirst()
                .orElseGet(() -> {
                    BrokerProfile fallback = profiles.getFirst();
                    PREFS.put(ACTIVE_PROFILE_KEY, fallback.id());
                    return fallback;
                });
    }

    public void setActiveProfile(String profileId) {
        if (profileId != null && loadProfileIds().contains(profileId)) {
            PREFS.put(ACTIVE_PROFILE_KEY, profileId);
        }
    }

    public void saveProfile(BrokerProfile profile) {
        List<String> ids = new ArrayList<>(loadProfileIds());
        if (!ids.contains(profile.id())) {
            ids.add(profile.id());
            saveProfileIds(ids);
        }
        writeProfile(profile);
    }

    public BrokerProfile createProfile(String name) {
        String id = "custom-" + UUID.randomUUID();
        BrokerProfile profile = new BrokerProfile(
                id,
                name.trim(),
                TradeCurrency.EUR,
                CommissionBasis.TRADE_VALUE_PERCENT,
                0.0,
                0.0,
                MaximumType.NONE,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                false,
                26.0
        );
        saveProfile(profile);
        return profile;
    }

    public boolean deleteProfile(String profileId) {
        List<String> ids = new ArrayList<>(loadProfileIds());
        if (ids.size() <= 1 || !ids.remove(profileId)) {
            return false;
        }

        try {
            profileNode(profileId).removeNode();
        } catch (BackingStoreException ignored) {
            return false;
        }

        saveProfileIds(ids);
        if (profileId.equals(PREFS.get(ACTIVE_PROFILE_KEY, ""))) {
            PREFS.put(ACTIVE_PROFILE_KEY, ids.getFirst());
        }
        return true;
    }

    private void ensureProfiles() {
        int currentVersion = PREFS.getInt(SCHEMA_VERSION_KEY, 0);
        if (currentVersion >= SCHEMA_VERSION && !loadProfileIds().isEmpty()) {
            return;
        }
        migrateToSchema2();
    }

    private void migrateToSchema2() {
        List<String> oldIds = new ArrayList<>(loadProfileIds());
        List<BrokerProfile> customProfiles = new ArrayList<>();

        for (String id : oldIds) {
            if (LEGACY_FINECO_ID.equals(id) || LEGACY_IBKR_ID.equals(id) || isPresetId(id)) {
                continue;
            }
            BrokerProfile migrated = loadLegacyOrCurrentCustomProfile(id);
            if (migrated != null) {
                customProfiles.add(migrated);
            }
        }

        List<BrokerProfile> defaults = defaultProfiles();
        List<String> ids = new ArrayList<>();
        for (BrokerProfile profile : defaults) {
            ids.add(profile.id());
            writeProfile(profile);
        }
        for (BrokerProfile profile : customProfiles) {
            ids.add(profile.id());
            writeProfile(profile);
        }
        saveProfileIds(ids);

        String oldActive = PREFS.get(ACTIVE_PROFILE_KEY, FINECO_ITALY_ID);
        String newActive = switch (oldActive) {
            case LEGACY_FINECO_ID -> FINECO_ITALY_ID;
            case LEGACY_IBKR_ID -> IBKR_EU_TIERED_ID;
            default -> ids.contains(oldActive) ? oldActive : FINECO_ITALY_ID;
        };
        PREFS.put(ACTIVE_PROFILE_KEY, newActive);
        PREFS.putInt(SCHEMA_VERSION_KEY, SCHEMA_VERSION);
    }

    private List<BrokerProfile> defaultProfiles() {
        return List.of(
                // Fineco Conto Trading: 0,19%, min 2,95 EUR, max 19 EUR.
                new BrokerProfile(
                        FINECO_ITALY_ID,
                        "Fineco Italia — Conto Trading",
                        TradeCurrency.EUR,
                        CommissionBasis.TRADE_VALUE_PERCENT,
                        0.19,
                        2.95,
                        MaximumType.FIXED_AMOUNT,
                        19.0,
                        0.0, 0.0,
                        0.0, 0.0,
                        0.0, 0.0,
                        false,
                        26.0
                ),
                // IBKR Europe, primo scaglione mensile, azioni denominate in EUR.
                new BrokerProfile(
                        IBKR_EU_TIERED_ID,
                        "IBKR Europa — Tiered",
                        TradeCurrency.EUR,
                        CommissionBasis.TRADE_VALUE_PERCENT,
                        0.05,
                        1.25,
                        MaximumType.FIXED_AMOUNT,
                        29.0,
                        0.0, 0.0,
                        0.0, 0.0,
                        0.0, 0.0,
                        true,
                        26.0
                ),
                new BrokerProfile(
                        IBKR_EU_FIXED_ID,
                        "IBKR Europa — Fixed SmartRouting",
                        TradeCurrency.EUR,
                        CommissionBasis.TRADE_VALUE_PERCENT,
                        0.05,
                        3.0,
                        MaximumType.NONE,
                        0.0,
                        0.0, 0.0,
                        0.0, 0.0,
                        0.0, 0.0,
                        false,
                        26.0
                ),
                // IBKR USA Tiered, primo scaglione (fino a 300.000 azioni/mese).
                // Costi noti inclusi: NSCC/DTC 0,00020/azione, CAT 0,000003/azione,
                // FINRA TAF 0,000195/azione in vendita e SEC 0,00206% del venduto.
                // Restano variabili le fee della sede di esecuzione e della liquidità.
                new BrokerProfile(
                        IBKR_US_TIERED_ID,
                        "IBKR USA — Tiered",
                        TradeCurrency.USD,
                        CommissionBasis.PER_SHARE,
                        0.0035,
                        0.35,
                        MaximumType.TRADE_VALUE_PERCENT,
                        1.0,
                        0.0, 0.0,
                        0.000203, 0.000398,
                        0.0, 0.00206,
                        true,
                        26.0
                ),
                new BrokerProfile(
                        IBKR_US_FIXED_ID,
                        "IBKR USA — Fixed",
                        TradeCurrency.USD,
                        CommissionBasis.PER_SHARE,
                        0.005,
                        1.0,
                        MaximumType.TRADE_VALUE_PERCENT,
                        1.0,
                        0.0, 0.0,
                        0.0, 0.0,
                        0.0, 0.0,
                        false,
                        26.0
                )
        );
    }

    private boolean isPresetId(String id) {
        return FINECO_ITALY_ID.equals(id)
                || IBKR_EU_TIERED_ID.equals(id)
                || IBKR_EU_FIXED_ID.equals(id)
                || IBKR_US_TIERED_ID.equals(id)
                || IBKR_US_FIXED_ID.equals(id);
    }

    private BrokerProfile loadLegacyOrCurrentCustomProfile(String id) {
        Preferences node = profileNode(id);
        String name = node.get("name", "").trim();
        if (name.isEmpty()) {
            return null;
        }

        if (node.get("commission.basis", null) != null) {
            return loadProfile(id);
        }

        double legacyBuy = node.getDouble("commission.buy", 0.0);
        double legacySell = node.getDouble("commission.sell", 0.0);
        return new BrokerProfile(
                id,
                name,
                TradeCurrency.EUR,
                CommissionBasis.TRADE_VALUE_PERCENT,
                0.0,
                0.0,
                MaximumType.NONE,
                0.0,
                legacyBuy,
                legacySell,
                0.0,
                0.0,
                0.0,
                0.0,
                false,
                node.getDouble("tax.rate", 26.0)
        );
    }

    private BrokerProfile loadProfile(String id) {
        Preferences node = profileNode(id);
        String name = node.get("name", "").trim();
        if (name.isEmpty()) {
            return null;
        }

        return new BrokerProfile(
                id,
                name,
                enumValue(TradeCurrency.class, node.get("currency", "EUR"), TradeCurrency.EUR),
                enumValue(CommissionBasis.class, node.get("commission.basis", "TRADE_VALUE_PERCENT"), CommissionBasis.TRADE_VALUE_PERCENT),
                node.getDouble("commission.rate", 0.0),
                node.getDouble("commission.minimum", 0.0),
                enumValue(MaximumType.class, node.get("commission.maximum.type", "NONE"), MaximumType.NONE),
                node.getDouble("commission.maximum.value", 0.0),
                node.getDouble("extra.buy.fixed", 0.0),
                node.getDouble("extra.sell.fixed", 0.0),
                node.getDouble("extra.buy.perShare", 0.0),
                node.getDouble("extra.sell.perShare", 0.0),
                node.getDouble("extra.buy.percent", 0.0),
                node.getDouble("extra.sell.percent", 0.0),
                node.getBoolean("fees.variable", false),
                node.getDouble("tax.rate", 26.0)
        );
    }

    private void writeProfile(BrokerProfile profile) {
        Preferences node = profileNode(profile.id());
        node.put("name", profile.name().trim());
        node.put("currency", profile.currency().name());
        node.put("commission.basis", profile.commissionBasis().name());
        node.putDouble("commission.rate", profile.rate());
        node.putDouble("commission.minimum", profile.minimum());
        node.put("commission.maximum.type", profile.maximumType().name());
        node.putDouble("commission.maximum.value", profile.maximumValue());
        node.putDouble("extra.buy.fixed", profile.buyExtraFixed());
        node.putDouble("extra.sell.fixed", profile.sellExtraFixed());
        node.putDouble("extra.buy.perShare", profile.buyExtraPerShare());
        node.putDouble("extra.sell.perShare", profile.sellExtraPerShare());
        node.putDouble("extra.buy.percent", profile.buyExtraPercent());
        node.putDouble("extra.sell.percent", profile.sellExtraPercent());
        node.putBoolean("fees.variable", profile.variableExternalFees());
        node.putDouble("tax.rate", profile.taxRate());
    }

    private Preferences profileNode(String id) {
        return PREFS.node(PROFILE_NODE).node(id);
    }

    private List<String> loadProfileIds() {
        String raw = PREFS.get(PROFILE_IDS_KEY, "").trim();
        if (raw.isEmpty()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();
    }

    private void saveProfileIds(List<String> ids) {
        PREFS.put(PROFILE_IDS_KEY, String.join(",", ids));
    }

    private <E extends Enum<E>> E enumValue(Class<E> type, String value, E fallback) {
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException | NullPointerException ex) {
            return fallback;
        }
    }
}

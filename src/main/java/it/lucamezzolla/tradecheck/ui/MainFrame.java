package it.lucamezzolla.tradecheck.ui;

import it.lucamezzolla.tradecheck.i18n.I18n;
import it.lucamezzolla.tradecheck.model.BrokerProfile;
import it.lucamezzolla.tradecheck.service.SettingsService;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Locale;

@SuppressWarnings("serial")
public final class MainFrame extends JFrame {
    private final SettingsService settingsService = new SettingsService();
    private final PurchasePreviewPanel previewPanel = new PurchasePreviewPanel(settingsService);
    private final CompletedTradePanel completedTradePanel = new CompletedTradePanel(settingsService);

    public MainFrame() {
        super(I18n.text("app.title"));
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1280, 760));
        setLocationByPlatform(true);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab(I18n.text("tab.preview"), previewPanel);
        tabs.addTab(I18n.text("tab.completed"), completedTradePanel);

        JButton options = new JButton(I18n.text("button.options"));
        options.addActionListener(e -> new OptionsDialog(this).setVisible(true));

        JComboBox<BrokerProfile> profileSelector = createProfileSelector();
        JComboBox<LanguageItem> languageSelector = createLanguageSelector();

        JPanel top = new JPanel(new BorderLayout());
        JLabel title = new JLabel(I18n.text("app.title"));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        title.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 10));
        top.add(title, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        right.add(new JLabel(I18n.text("main.platform")));
        right.add(profileSelector);
        right.add(new JSeparator(SwingConstants.VERTICAL));
        right.add(new JLabel(I18n.text("main.language")));
        right.add(languageSelector);
        right.add(options);
        top.add(right, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        pack();
    }

    private JComboBox<BrokerProfile> createProfileSelector() {
        List<BrokerProfile> profiles = settingsService.loadProfiles();
        JComboBox<BrokerProfile> selector = new JComboBox<>(profiles.toArray(BrokerProfile[]::new));
        BrokerProfile activeProfile = settingsService.loadActiveProfile();
        selectProfile(selector, activeProfile.id());

        selector.addActionListener(e -> {
            BrokerProfile selected = (BrokerProfile) selector.getSelectedItem();
            if (selected == null) {
                return;
            }
            settingsService.setActiveProfile(selected.id());
            previewPanel.applyProfile(selected);
            completedTradePanel.applyProfile(selected);
        });
        return selector;
    }

    private JComboBox<LanguageItem> createLanguageSelector() {
        JComboBox<LanguageItem> selector = new JComboBox<>(new LanguageItem[] {
                new LanguageItem("English", Locale.ENGLISH),
                new LanguageItem("Italiano", Locale.ITALIAN),
                new LanguageItem("Español", Locale.forLanguageTag("es")),
                new LanguageItem("Français", Locale.FRENCH),
                new LanguageItem("Português", Locale.forLanguageTag("pt"))
        });

        for (int i = 0; i < selector.getItemCount(); i++) {
            if (selector.getItemAt(i).locale().getLanguage()
                    .equalsIgnoreCase(I18n.getLocale().getLanguage())) {
                selector.setSelectedIndex(i);
                break;
            }
        }

        selector.addActionListener(e -> {
            LanguageItem selected = (LanguageItem) selector.getSelectedItem();
            if (selected == null || selected.locale().getLanguage()
                    .equalsIgnoreCase(I18n.getLocale().getLanguage())) {
                return;
            }
            I18n.setLocale(selected.locale());
            dispose();
            SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
        });
        return selector;
    }

    private void selectProfile(JComboBox<BrokerProfile> selector, String profileId) {
        for (int i = 0; i < selector.getItemCount(); i++) {
            if (selector.getItemAt(i).id().equals(profileId)) {
                selector.setSelectedIndex(i);
                return;
            }
        }
    }

    private record LanguageItem(String label, Locale locale) {
        @Override
        public String toString() {
            return label;
        }
    }
}

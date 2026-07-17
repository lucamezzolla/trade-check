package it.lucamezzolla.tradecheck.ui;

import it.lucamezzolla.tradecheck.i18n.I18n;
import it.lucamezzolla.tradecheck.model.BrokerProfile;
import it.lucamezzolla.tradecheck.model.CommissionBasis;
import it.lucamezzolla.tradecheck.model.MaximumType;
import it.lucamezzolla.tradecheck.model.TradeCurrency;
import it.lucamezzolla.tradecheck.service.SettingsService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

@SuppressWarnings("serial")
public final class OptionsDialog extends JDialog {
    private final JFrame ownerFrame;
    private final JComboBox<BrokerProfile> profileSelector = new JComboBox<>();
    private final JTextField profileName = new JTextField();
    private final JComboBox<TradeCurrency> currency = new JComboBox<>(TradeCurrency.values());
    private final JComboBox<CommissionBasis> commissionBasis = new JComboBox<>(CommissionBasis.values());
    private final JTextField rate = new JTextField();
    private final JTextField minimum = new JTextField();
    private final JComboBox<MaximumType> maximumType = new JComboBox<>(MaximumType.values());
    private final JTextField maximumValue = new JTextField();
    private final JTextField buyExtraFixed = new JTextField();
    private final JTextField sellExtraFixed = new JTextField();
    private final JTextField buyExtraPerShare = new JTextField();
    private final JTextField sellExtraPerShare = new JTextField();
    private final JTextField buyExtraPercent = new JTextField();
    private final JTextField sellExtraPercent = new JTextField();
    private final JCheckBox variableExternalFees = new JCheckBox(I18n.text("options.variableExternalFees"));
    private final JTextField taxRate = new JTextField();
    private final SettingsService settingsService = new SettingsService();

    public OptionsDialog(JFrame owner) {
        super(owner, I18n.text("options.title"), true);
        this.ownerFrame = owner;
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setSize(980, 760);
        setMinimumSize(new Dimension(900, 680));
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeAndRefresh();
            }
        });

        configureEnumRenderers();
        reloadProfiles(settingsService.loadActiveProfile().id());
        profileSelector.addActionListener(e -> loadSelectedProfile());
        commissionBasis.addActionListener(e -> updateFieldState());
        maximumType.addActionListener(e -> updateFieldState());

        JButton addProfile = new JButton(I18n.text("button.newProfile"));
        addProfile.addActionListener(e -> createProfile());
        JButton deleteProfile = new JButton(I18n.text("button.deleteProfile"));
        deleteProfile.addActionListener(e -> deleteProfile());

        JPanel profileActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        profileActions.add(addProfile);
        profileActions.add(deleteProfile);

        JPanel profileRow = new JPanel(new BorderLayout(8, 0));
        profileRow.add(profileSelector, BorderLayout.CENTER);
        profileRow.add(profileActions, BorderLayout.EAST);

        JPanel profileForm = verticalForm();
        profileForm.setBorder(BorderFactory.createTitledBorder(I18n.text("options.section.profile")));
        profileForm.add(UiSupport.row(I18n.text("options.profile"), profileRow));
        profileForm.add(UiSupport.row(I18n.text("options.profileName"), profileName));
        profileForm.add(UiSupport.row(I18n.text("options.currency"), currency));

        JPanel baseForm = verticalForm();
        baseForm.setBorder(BorderFactory.createTitledBorder(I18n.text("options.section.baseCommission")));
        baseForm.add(UiSupport.row(I18n.text("options.commissionBasis"), commissionBasis));
        baseForm.add(UiSupport.row(I18n.text("options.rate"), I18n.text("options.rate.tooltip"), rate));
        baseForm.add(UiSupport.row(I18n.text("options.minimum"), minimum));
        baseForm.add(UiSupport.row(I18n.text("options.maximumType"), maximumType));
        baseForm.add(UiSupport.row(I18n.text("options.maximumValue"), I18n.text("options.maximumValue.tooltip"), maximumValue));

        JPanel extrasForm = verticalForm();
        extrasForm.setBorder(BorderFactory.createTitledBorder(I18n.text("options.section.extraCosts")));
        extrasForm.add(UiSupport.row(I18n.text("options.buyExtraFixed"), buyExtraFixed));
        extrasForm.add(UiSupport.row(I18n.text("options.sellExtraFixed"), sellExtraFixed));
        extrasForm.add(UiSupport.row(I18n.text("options.buyExtraPerShare"), buyExtraPerShare));
        extrasForm.add(UiSupport.row(I18n.text("options.sellExtraPerShare"), sellExtraPerShare));
        extrasForm.add(UiSupport.row(I18n.text("options.buyExtraPercent"), buyExtraPercent));
        extrasForm.add(UiSupport.row(I18n.text("options.sellExtraPercent"), sellExtraPercent));
        variableExternalFees.setToolTipText(I18n.text("options.variableExternalFees.tooltip"));
        JPanel variableRow = new JPanel(new BorderLayout());
        variableRow.add(variableExternalFees, BorderLayout.CENTER);
        extrasForm.add(variableRow);

        JPanel taxForm = verticalForm();
        taxForm.setBorder(BorderFactory.createTitledBorder(I18n.text("options.section.tax")));
        taxForm.add(UiSupport.row(I18n.text("options.taxRate"), taxRate));

        JPanel form = verticalForm();
        form.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        form.add(profileForm);
        form.add(Box.createVerticalStrut(8));
        form.add(baseForm);
        form.add(Box.createVerticalStrut(8));
        form.add(extrasForm);
        form.add(Box.createVerticalStrut(8));
        form.add(taxForm);

        JLabel note = new JLabel("<html>" + I18n.text("options.commissionNote") + "</html>");
        note.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));

        JButton save = new JButton(I18n.text("button.save"));
        save.addActionListener(e -> save());
        JButton cancel = new JButton(I18n.text("button.cancel"));
        cancel.addActionListener(e -> closeAndRefresh());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancel);
        buttons.add(save);

        JPanel scrollContent = new ViewportWidthPanel(new BorderLayout());
        scrollContent.add(form, BorderLayout.NORTH);
        scrollContent.add(note, BorderLayout.SOUTH);
        JScrollPane scrollPane = new JScrollPane(scrollContent);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        loadSelectedProfile();
    }

    /**
     * Keeps the form aligned to the visible viewport width. Without this, the
     * preferred width of long labels and fields can create an unnecessary
     * horizontal scrollbar, especially with the macOS look and feel.
     */
    private static final class ViewportWidthPanel extends JPanel implements Scrollable {
        private ViewportWidthPanel(LayoutManager layout) {
            super(layout);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(16, visibleRect.height - 16);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    private JPanel verticalForm() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }

    private void configureEnumRenderers() {
        commissionBasis.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof CommissionBasis basis) {
                    setText(I18n.text(basis == CommissionBasis.TRADE_VALUE_PERCENT
                            ? "commissionBasis.tradeValuePercent"
                            : "commissionBasis.perShare"));
                }
                return this;
            }
        });

        maximumType.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof MaximumType type) {
                    String key = switch (type) {
                        case NONE -> "maximumType.none";
                        case FIXED_AMOUNT -> "maximumType.fixed";
                        case TRADE_VALUE_PERCENT -> "maximumType.tradeValuePercent";
                    };
                    setText(I18n.text(key));
                }
                return this;
            }
        });
    }

    private void save() {
        try {
            BrokerProfile selected = (BrokerProfile) profileSelector.getSelectedItem();
            if (selected == null || profileName.getText().trim().isEmpty()) {
                throw new NumberFormatException();
            }

            BrokerProfile updated = new BrokerProfile(
                    selected.id(),
                    profileName.getText().trim(),
                    (TradeCurrency) currency.getSelectedItem(),
                    (CommissionBasis) commissionBasis.getSelectedItem(),
                    UiSupport.number(rate),
                    UiSupport.number(minimum),
                    (MaximumType) maximumType.getSelectedItem(),
                    UiSupport.number(maximumValue),
                    UiSupport.number(buyExtraFixed),
                    UiSupport.number(sellExtraFixed),
                    UiSupport.number(buyExtraPerShare),
                    UiSupport.number(sellExtraPerShare),
                    UiSupport.number(buyExtraPercent),
                    UiSupport.number(sellExtraPercent),
                    variableExternalFees.isSelected(),
                    UiSupport.number(taxRate)
            );
            validate(updated);

            settingsService.saveProfile(updated);
            settingsService.setActiveProfile(updated.id());
            JOptionPane.showMessageDialog(this, I18n.text("options.savedMessage"));
            closeAndRefresh();
        } catch (NumberFormatException | NullPointerException ex) {
            JOptionPane.showMessageDialog(this, I18n.text("error.invalidNumber"),
                    I18n.text("error.title"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void validate(BrokerProfile profile) {
        if (profile.rate() < 0
                || profile.minimum() < 0
                || profile.maximumValue() < 0
                || profile.buyExtraFixed() < 0
                || profile.sellExtraFixed() < 0
                || profile.buyExtraPerShare() < 0
                || profile.sellExtraPerShare() < 0
                || profile.buyExtraPercent() < 0
                || profile.sellExtraPercent() < 0
                || profile.taxRate() < 0
                || profile.taxRate() > 100) {
            throw new NumberFormatException();
        }
    }

    private void createProfile() {
        String name = JOptionPane.showInputDialog(
                this,
                I18n.text("options.newProfilePrompt"),
                I18n.text("button.newProfile"),
                JOptionPane.PLAIN_MESSAGE
        );
        if (name == null || name.trim().isEmpty()) {
            return;
        }

        BrokerProfile created = settingsService.createProfile(name);
        reloadProfiles(created.id());
        loadSelectedProfile();
    }

    private void deleteProfile() {
        BrokerProfile selected = (BrokerProfile) profileSelector.getSelectedItem();
        if (selected == null) {
            return;
        }

        int answer = JOptionPane.showConfirmDialog(
                this,
                I18n.text("options.deleteProfileConfirm") + "\n" + selected.name(),
                I18n.text("button.deleteProfile"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }

        if (!settingsService.deleteProfile(selected.id())) {
            JOptionPane.showMessageDialog(this, I18n.text("options.cannotDeleteLastProfile"),
                    I18n.text("error.title"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        BrokerProfile active = settingsService.loadActiveProfile();
        reloadProfiles(active.id());
        loadSelectedProfile();
    }

    private void reloadProfiles(String selectedId) {
        List<BrokerProfile> profiles = settingsService.loadProfiles();
        DefaultComboBoxModel<BrokerProfile> model = new DefaultComboBoxModel<>();
        profiles.forEach(model::addElement);
        profileSelector.setModel(model);

        for (int i = 0; i < model.getSize(); i++) {
            if (model.getElementAt(i).id().equals(selectedId)) {
                profileSelector.setSelectedIndex(i);
                return;
            }
        }
        if (model.getSize() > 0) {
            profileSelector.setSelectedIndex(0);
        }
    }

    private void loadSelectedProfile() {
        BrokerProfile selected = (BrokerProfile) profileSelector.getSelectedItem();
        if (selected == null) {
            return;
        }
        profileName.setText(selected.name());
        currency.setSelectedItem(selected.currency());
        commissionBasis.setSelectedItem(selected.commissionBasis());
        rate.setText(UiSupport.editableNumber(selected.rate()));
        minimum.setText(UiSupport.editableNumber(selected.minimum()));
        maximumType.setSelectedItem(selected.maximumType());
        maximumValue.setText(UiSupport.editableNumber(selected.maximumValue()));
        buyExtraFixed.setText(UiSupport.editableNumber(selected.buyExtraFixed()));
        sellExtraFixed.setText(UiSupport.editableNumber(selected.sellExtraFixed()));
        buyExtraPerShare.setText(UiSupport.editableNumber(selected.buyExtraPerShare()));
        sellExtraPerShare.setText(UiSupport.editableNumber(selected.sellExtraPerShare()));
        buyExtraPercent.setText(UiSupport.editableNumber(selected.buyExtraPercent()));
        sellExtraPercent.setText(UiSupport.editableNumber(selected.sellExtraPercent()));
        variableExternalFees.setSelected(selected.variableExternalFees());
        taxRate.setText(UiSupport.editableNumber(selected.taxRate()));
        updateFieldState();
    }

    private void updateFieldState() {
        MaximumType type = (MaximumType) maximumType.getSelectedItem();
        maximumValue.setEnabled(type != null && type != MaximumType.NONE);
        CommissionBasis basis = (CommissionBasis) commissionBasis.getSelectedItem();
        rate.setToolTipText(I18n.text(basis == CommissionBasis.PER_SHARE
                ? "options.rate.perShare.tooltip"
                : "options.rate.percent.tooltip"));
    }

    private void closeAndRefresh() {
        dispose();
        ownerFrame.dispose();
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}

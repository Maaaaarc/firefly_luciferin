/*
  DevicesTabController.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright © 2020 - 2023  Davide Perini  (https://github.com/sblantipodi)

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
package org.dpsoftware.gui.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.InputEvent;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.gui.elements.GlowWormDevice;
import org.dpsoftware.managers.DisplayManager;
import org.dpsoftware.managers.NetworkManager;
import org.dpsoftware.managers.dto.FirmwareConfigDto;
import org.dpsoftware.utilities.CommonUtility;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Devices Tab controller
 */
@Slf4j
public class DevicesTabController {

    public static ObservableList<GlowWormDevice> deviceTableData = FXCollections.observableArrayList();
    public static ObservableList<GlowWormDevice> deviceTableDataTemp = FXCollections.observableArrayList();
    public static boolean oldFirmwareDevice = false;
    // FXML binding
    @FXML
    public CheckBox checkForUpdates;
    @FXML
    public CheckBox multiScreenSingleDevice;
    @FXML
    public Button saveDeviceButton;
    @FXML
    public ComboBox<String> powerSaving;
    @FXML
    public ComboBox<String> multiMonitor;
    @FXML
    public CheckBox syncCheck;
    @FXML
    public TableColumn<GlowWormDevice, String> gpioClockColumn;
    boolean cellEdit = false;
    // Inject main controller
    @FXML
    private SettingsController settingsController;
    @FXML
    private TableView<GlowWormDevice> deviceTable;
    @FXML
    private TableColumn<GlowWormDevice, String> deviceNameColumn;
    @FXML
    private TableColumn<GlowWormDevice, String> deviceBoardColumn;
    @FXML
    private TableColumn<GlowWormDevice, Hyperlink> deviceIPColumn;
    @FXML
    private TableColumn<GlowWormDevice, String> deviceVersionColumn;
    @FXML
    private TableColumn<GlowWormDevice, String> wifiColumn;
    @FXML
    private TableColumn<GlowWormDevice, String> macColumn;
    @FXML
    private TableColumn<GlowWormDevice, String> gpioColumn;
    @FXML
    private TableColumn<GlowWormDevice, String> firmwareColumn;
    @FXML
    private TableColumn<GlowWormDevice, String> baudrateColumn;
    @FXML
    private TableColumn<GlowWormDevice, String> mqttTopicColumn;
    @FXML
    private TableColumn<GlowWormDevice, String> numberOfLEDSconnectedColumn;
    @FXML
    private TableColumn<GlowWormDevice, String> colorModeColumn;
    @FXML
    private TableColumn<GlowWormDevice, String> colorOrderColumn;
    @FXML
    private TableColumn<GlowWormDevice, String> ldrColumn;
    @FXML
    private TableColumn<GlowWormDevice, String> ldrPinColumn;
    @FXML
    private TableColumn<GlowWormDevice, String> relayPinColumn;
    @FXML
    private TableColumn<GlowWormDevice, String> sbPinColumn;
    @FXML
    private Label versionLabel;

    /**
     * Inject main controller containing the TabPane
     *
     * @param settingsController TabPane controller
     */
    public void injectSettingsController(SettingsController settingsController) {
        this.settingsController = settingsController;
    }

    /**
     * Initialize controller with system's specs
     */
    @FXML
    protected void initialize() {
        // Device table
        deviceNameColumn.setCellValueFactory(cellData -> cellData.getValue().deviceNameProperty());
        deviceBoardColumn.setCellValueFactory(cellData -> cellData.getValue().deviceBoardProperty());
        deviceIPColumn.setCellFactory(e -> new TableCell<>() {
            @Override
            protected void updateItem(Hyperlink item, boolean empty) {
                super.updateItem(item, empty);
                final Hyperlink link;
                if (!empty) {
                    GlowWormDevice glowWormDevice = getTableRow().getItem();
                    if (glowWormDevice != null) {
                        link = new Hyperlink(item != null ? item.getText() : glowWormDevice.getDeviceIP());
                        if (glowWormDevice.getWifi().contains(Constants.DASH)) {
                            link.setStyle(Constants.CSS_NO_UNDERLINE + Constants.TC_NO_BOLD_TEXT);
                        } else {
                            link.setOnAction(evt -> FireflyLuciferin.guiManager.surfToURL(Constants.HTTP + getTableRow().getItem().getDeviceIP()));
                        }
                        setGraphic(link);
                    }
                }
            }
        });
        deviceVersionColumn.setCellValueFactory(cellData -> cellData.getValue().deviceVersionProperty());
        wifiColumn.setCellValueFactory(cellData -> cellData.getValue().wifiProperty());
        macColumn.setCellValueFactory(cellData -> cellData.getValue().macProperty());
        gpioColumn.setCellValueFactory(cellData -> cellData.getValue().gpioProperty());
        gpioColumn.setStyle(Constants.TC_BOLD_TEXT + Constants.CSS_UNDERLINE);
        firmwareColumn.setCellValueFactory(cellData -> cellData.getValue().firmwareTypeProperty());
        baudrateColumn.setCellValueFactory(cellData -> cellData.getValue().baudRateProperty());
        mqttTopicColumn.setCellValueFactory(cellData -> cellData.getValue().mqttTopicProperty());
        colorModeColumn.setCellValueFactory(cellData -> cellData.getValue().colorModeProperty());
        colorOrderColumn.setCellFactory(tc -> new ComboBoxTableCell<>(Enums.ColorOrder.GRB.name(), Enums.ColorOrder.RGB.name(),
                Enums.ColorOrder.BGR.name(), Enums.ColorOrder.BRG.name(), Enums.ColorOrder.RBG.name(), Enums.ColorOrder.GBR.name()));
        colorOrderColumn.setCellValueFactory(cellData -> cellData.getValue().colorOrderProperty());
        colorOrderColumn.setStyle(Constants.TC_BOLD_TEXT + Constants.CSS_UNDERLINE);
        colorOrderColumn.setOnEditStart((TableColumn.CellEditEvent<GlowWormDevice, String> t) -> cellEdit = true);
        colorOrderColumn.setOnEditCancel((TableColumn.CellEditEvent<GlowWormDevice, String> t) -> cellEdit = false);
        colorOrderColumn.setOnEditCommit((TableColumn.CellEditEvent<GlowWormDevice, String> t) -> {
            cellEdit = false;
            GlowWormDevice device = t.getTableView().getItems().get(t.getTablePosition().getRow());
            log.info("Setting Color Order" + t.getNewValue() + " on " + device.getDeviceName());
            device.setColorOrder(t.getNewValue());
            if (FireflyLuciferin.guiManager != null) {
                FireflyLuciferin.guiManager.stopCapturingThreads(true);
            }
            if (FireflyLuciferin.config != null && FireflyLuciferin.config.isFullFirmware()) {
                FirmwareConfigDto firmwareConfigDto = new FirmwareConfigDto();
                firmwareConfigDto.setColorOrder(String.valueOf(Enums.ColorOrder.valueOf(t.getNewValue()).getValue()));
                firmwareConfigDto.setMAC(device.getMac());
                NetworkManager.publishToTopic(NetworkManager.getTopic(Constants.GLOW_WORM_FIRM_CONFIG_TOPIC),
                        CommonUtility.toJsonString(firmwareConfigDto));
            } else if (FireflyLuciferin.config != null) {
                FireflyLuciferin.colorOrder = Enums.ColorOrder.valueOf(t.getNewValue()).getValue();
                settingsController.sendSerialParams();
            }
        });
        ldrColumn.setCellValueFactory(cellData -> cellData.getValue().ldrValueProperty());
        ldrPinColumn.setCellValueFactory(cellData -> cellData.getValue().ldrPinProperty());
        ldrPinColumn.setStyle(Constants.TC_BOLD_TEXT + Constants.CSS_UNDERLINE);
        ldrPinColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        ldrPinColumn.setOnEditStart((TableColumn.CellEditEvent<GlowWormDevice, String> t) -> cellEdit = true);
        ldrPinColumn.setOnEditCancel((TableColumn.CellEditEvent<GlowWormDevice, String> t) -> cellEdit = false);
        ldrPinColumn.setOnEditCommit(this::setPins);
        relayPinColumn.setCellValueFactory(cellData -> cellData.getValue().relayPinProperty());
        relayPinColumn.setStyle(Constants.TC_BOLD_TEXT + Constants.CSS_UNDERLINE);
        relayPinColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        relayPinColumn.setOnEditStart((TableColumn.CellEditEvent<GlowWormDevice, String> t) -> cellEdit = true);
        relayPinColumn.setOnEditCancel((TableColumn.CellEditEvent<GlowWormDevice, String> t) -> cellEdit = false);
        relayPinColumn.setOnEditCommit(this::setPins);
        sbPinColumn.setCellValueFactory(cellData -> cellData.getValue().sbPinProperty());
        sbPinColumn.setStyle(Constants.TC_BOLD_TEXT + Constants.CSS_UNDERLINE);
        sbPinColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        sbPinColumn.setOnEditStart((TableColumn.CellEditEvent<GlowWormDevice, String> t) -> cellEdit = true);
        sbPinColumn.setOnEditCancel((TableColumn.CellEditEvent<GlowWormDevice, String> t) -> cellEdit = false);
        sbPinColumn.setOnEditCommit(this::setPins);
        gpioClockColumn.setCellValueFactory(cellData -> cellData.getValue().gpioClockProperty());
        gpioClockColumn.setStyle(Constants.TC_BOLD_TEXT + Constants.CSS_UNDERLINE);
        gpioClockColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        gpioClockColumn.setOnEditStart((TableColumn.CellEditEvent<GlowWormDevice, String> t) -> cellEdit = true);
        gpioClockColumn.setOnEditCancel((TableColumn.CellEditEvent<GlowWormDevice, String> t) -> cellEdit = false);
        gpioClockColumn.setOnEditCommit(this::setPins);
        numberOfLEDSconnectedColumn.setCellValueFactory(cellData -> cellData.getValue().numberOfLEDSconnectedProperty());
        deviceTable.setEditable(true);
        deviceTable.setItems(getDeviceTableData());
    }

    /**
     * Set GPIO pins: relay pin, smart button pin, ldr pin
     *
     * @param t device table row
     */
    private void setPins(TableColumn.CellEditEvent<GlowWormDevice, String> t) {
        cellEdit = false;
        GlowWormDevice device = t.getTableView().getItems().get(t.getTablePosition().getRow());
        Optional<ButtonType> result = FireflyLuciferin.guiManager.showLocalizedAlert(Constants.GPIO_OK_TITLE, Constants.GPIO_OK_HEADER,
                Constants.GPIO_OK_CONTEXT, Alert.AlertType.CONFIRMATION);
        ButtonType button = result.orElse(ButtonType.OK);
        if (button == ButtonType.OK) {
            String pinToEdit = t.getTableColumn().getText();
            log.info("Setting " + pinToEdit + " " + t.getNewValue() + " on " + device.getDeviceName());
            if (t.getTableColumn().getId().equals(Constants.EDITABLE_PIN_LDRPIN)) {
                device.setLdrPin(t.getNewValue());
            } else if (t.getTableColumn().getId().equals(Constants.EDITABLE_PIN_RELAYPIN)) {
                device.setRelayPin(t.getNewValue());
            } else if (t.getTableColumn().getId().equals(Constants.EDITABLE_PIN_SBPIN)) {
                device.setSbPin(t.getNewValue());
            } else if (t.getTableColumn().getId().equals(Constants.EDITABLE_PIN_GPIO_CLOCK)) {
                device.setGpioClock(t.getNewValue());
            }
            if (FireflyLuciferin.guiManager != null) {
                FireflyLuciferin.guiManager.stopCapturingThreads(true);
            }
            if (FireflyLuciferin.config != null && FireflyLuciferin.config.isFullFirmware()) {
                FirmwareConfigDto firmwareConfigDto = new FirmwareConfigDto();
                firmwareConfigDto.setMAC(device.getMac());
                firmwareConfigDto.setLdrPin(Integer.parseInt(device.getLdrPin()));
                firmwareConfigDto.setRelayPin(Integer.parseInt(device.getRelayPin()));
                firmwareConfigDto.setSbPin(Integer.parseInt(device.getSbPin()));
                firmwareConfigDto.setGpioClock(Integer.parseInt(device.getGpioClock()));
                NetworkManager.publishToTopic(NetworkManager.getTopic(Constants.GLOW_WORM_FIRM_CONFIG_TOPIC),
                        CommonUtility.toJsonString(firmwareConfigDto));
            } else if (FireflyLuciferin.config != null) {
                FireflyLuciferin.ldrPin = Integer.parseInt(device.getLdrPin());
                FireflyLuciferin.relayPin = Integer.parseInt(device.getRelayPin());
                FireflyLuciferin.sbPin = Integer.parseInt(device.getSbPin());
                FireflyLuciferin.gpioClockPin = Integer.parseInt(device.getGpioClock());
                settingsController.sendSerialParams();
            }
        }
    }

    /**
     * Init form values
     */
    void initDefaultValues() {
        versionLabel.setText(Constants.FIREFLY_LUCIFERIN + " (v" + FireflyLuciferin.version + ")");
        powerSaving.setValue(Enums.PowerSaving.DISABLED.getI18n());
        multiMonitor.setValue(CommonUtility.getWord(Constants.MULTIMONITOR_1));
        checkForUpdates.setSelected(true);
        syncCheck.setSelected(true);
        multiScreenSingleDevice.setSelected(false);
        DisplayManager displayManager = new DisplayManager();
        multiScreenSingleDevice.setDisable(displayManager.displayNumber() <= 1);
        deviceTable.setPlaceholder(new Label(CommonUtility.getWord(Constants.NO_DEVICE_FOUND)));
    }

    /**
     * Init form values by reading existing config file
     *
     * @param currentConfig stored config
     */
    public void initValuesFromSettingsFile(Configuration currentConfig) {
        versionLabel.setText(Constants.FIREFLY_LUCIFERIN + " (v" + FireflyLuciferin.version + ")");
        if (!currentConfig.getPowerSaving().isEmpty()) {
            powerSaving.setValue(LocalizedEnum.fromBaseStr(Enums.PowerSaving.class, currentConfig.getPowerSaving()).getI18n());
        } else {
            powerSaving.setValue(LocalizedEnum.fromBaseStr(Enums.PowerSaving.class, Enums.PowerSaving.DISABLED.getBaseI18n()).getI18n());
        }
        multiScreenSingleDevice.setDisable(false);
        switch (currentConfig.getMultiMonitor()) {
            case 2 -> multiMonitor.setValue(CommonUtility.getWord(Constants.MULTIMONITOR_2));
            case 3 -> multiMonitor.setValue(CommonUtility.getWord(Constants.MULTIMONITOR_3));
            default -> multiMonitor.setValue(CommonUtility.getWord(Constants.MULTIMONITOR_1));
        }
        DisplayManager displayManager = new DisplayManager();
        multiScreenSingleDevice.setDisable(displayManager.displayNumber() <= 1);
        checkForUpdates.setSelected(currentConfig.isCheckForUpdates());
        multiScreenSingleDevice.setSelected(CommonUtility.isSingleDeviceMultiScreen());
        syncCheck.setSelected(currentConfig.isSyncCheck());
    }

    /**
     * Init combo boxes
     */
    void initComboBox() {
        for (Enums.PowerSaving pwr : Enums.PowerSaving.values()) {
            powerSaving.getItems().add(pwr.getI18n());
        }
    }

    /**
     * Devices Table edit manager
     */
    public void setTableEdit() {
        gpioColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        gpioColumn.setOnEditStart(t -> cellEdit = true);
        gpioColumn.setOnEditCommit(t -> {
            cellEdit = false;
            GlowWormDevice device = t.getTableView().getItems().get(t.getTablePosition().getRow());
            Optional<ButtonType> result = FireflyLuciferin.guiManager.showLocalizedAlert(Constants.GPIO_OK_TITLE, Constants.GPIO_OK_HEADER,
                    Constants.GPIO_OK_CONTEXT, Alert.AlertType.CONFIRMATION);
            ButtonType button = result.orElse(ButtonType.OK);
            if (button == ButtonType.OK) {
                log.info("Setting GPIO" + t.getNewValue() + " on " + device.getDeviceName());
                device.setGpio(t.getNewValue());
                if (FireflyLuciferin.guiManager != null) {
                    FireflyLuciferin.guiManager.stopCapturingThreads(true);
                }
                if (FireflyLuciferin.config != null && FireflyLuciferin.config.isFullFirmware()) {
                    FirmwareConfigDto gpioDto = new FirmwareConfigDto();
                    gpioDto.setGpio(Integer.parseInt(t.getNewValue()));
                    gpioDto.setMAC(device.getMac());
                    NetworkManager.publishToTopic(NetworkManager.getTopic(Constants.GLOW_WORM_FIRM_CONFIG_TOPIC),
                            CommonUtility.toJsonString(gpioDto));
                } else if (FireflyLuciferin.config != null) {
                    FireflyLuciferin.gpio = Integer.parseInt(t.getNewValue());
                    settingsController.sendSerialParams();
                }
            }
        });
    }

    /**
     * Manage the device list tab update
     */
    public void manageDeviceList() {
        if (!cellEdit) {
            Calendar calendar = Calendar.getInstance();
            Calendar calendarTemp = Calendar.getInstance();
            ObservableList<GlowWormDevice> deviceTableDataToRemove = FXCollections.observableArrayList();
            AtomicBoolean showClockColumn = new AtomicBoolean(false);
            deviceTableData.forEach(glowWormDevice -> {
                if (Enums.ColorMode.DOTSTAR.name().equalsIgnoreCase(glowWormDevice.getColorMode())) {
                    showClockColumn.set(true);
                }
                calendar.setTime(new Date());
                calendarTemp.setTime(new Date());
                calendar.add(Calendar.SECOND, -20);
                calendarTemp.add(Calendar.SECOND, -60);
                try {
                    if (calendar.getTime().after(FireflyLuciferin.formatter.parse(glowWormDevice.getLastSeen()))
                            && FireflyLuciferin.formatter.parse(glowWormDevice.getLastSeen()).after(calendarTemp.getTime())) {
                        if (!(Constants.SERIAL_PORT_AUTO.equals(FireflyLuciferin.config.getOutputDevice())
                                && FireflyLuciferin.config.getMultiMonitor() > 1) && !oldFirmwareDevice) {
                            deviceTableDataToRemove.add(glowWormDevice);
                        }
                    }
                } catch (ParseException e) {
                    log.error(e.getMessage());
                }
            });
            // Temp list contains the removed devices, they will be readded if a microcontroller restart occurs, and if the capture is runnning.
            deviceTableDataTemp.addAll(deviceTableDataToRemove);
            deviceTableData.removeAll(deviceTableDataToRemove);
            gpioClockColumn.setVisible(showClockColumn.get());
            deviceTable.refresh();
        }
    }

    /**
     * Save button event
     *
     * @param e event
     */
    @FXML
    public void save(InputEvent e) {
        settingsController.save(e);
    }

    /**
     * Save button from main controller
     *
     * @param config stored config
     */
    @FXML
    public void save(Configuration config) {
        config.setPowerSaving(LocalizedEnum.fromStr(Enums.PowerSaving.class, powerSaving.getValue()).getBaseI18n());
        config.setMultiMonitor(multiMonitor.getSelectionModel().getSelectedIndex() + 1);
        config.setCheckForUpdates(checkForUpdates.isSelected());
        config.setMultiScreenSingleDevice(multiScreenSingleDevice.isSelected());
        config.setSyncCheck(syncCheck.isSelected());
    }

    /**
     * Set red button if a param requires Firefly restart
     */
    @FXML
    public void saveButtonHover() {
        settingsController.checkProfileDifferences();
    }

    /**
     * Set form tooltips
     *
     * @param currentConfig stored config
     */
    void setTooltips(Configuration currentConfig) {
        powerSaving.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_POWER_SAVING));
        multiMonitor.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_MULTIMONITOR));
        checkForUpdates.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_CHECK_UPDATES));
        syncCheck.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_SYNC_CHECK));
        if (currentConfig == null) {
            saveDeviceButton.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_SAVEDEVICEBUTTON_NULL));
        }
    }

    /**
     * Return the observable devices list
     *
     * @return devices list
     */
    public ObservableList<GlowWormDevice> getDeviceTableData() {
        return deviceTableData;
    }

    /**
     * Open browser to the GitHub project page
     */
    @FXML
    public void onMouseClickedGitHubLink() {
        FireflyLuciferin.guiManager.surfToURL(Constants.GITHUB_URL);
    }

}

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class PortScannerGUI extends Application {

    private TextField ipAddressField;
    private TextField portRangeField;
    private TextField threadNumberField;
    private Button scanButton;
    private Label resultLabel;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Igor Avi's Port Scanner");

        GridPane gridPane = new GridPane();
        gridPane.setPadding(new Insets(10));
        gridPane.setVgap(5);
        gridPane.setHgap(5);

        Label ipLabel = new Label("Target IP Address:");
        GridPane.setConstraints(ipLabel, 0, 0);
        ipAddressField = new TextField();
        GridPane.setConstraints(ipAddressField, 1, 0);

        Label portRangeLabel = new Label("Port Range:");
        GridPane.setConstraints(portRangeLabel, 0, 1);
        portRangeField = new TextField();
        GridPane.setConstraints(portRangeField, 1, 1);

        Label threadNumberLabel = new Label("Thread Number:");
        GridPane.setConstraints(threadNumberLabel, 0, 2);
        threadNumberField = new TextField();
        GridPane.setConstraints(threadNumberField, 1, 2);

        scanButton = new Button("Scan Ports");
        GridPane.setConstraints(scanButton, 0, 3);

        resultLabel = new Label();
        GridPane.setConstraints(resultLabel, 0, 4);

        scanButton.setOnAction(e -> {
            String ipAddress = ipAddressField.getText().trim();
            String portRange = portRangeField.getText().trim();
            String threadNumber = threadNumberField.getText().trim();
            if (ipAddress.isEmpty() || portRange.isEmpty() || threadNumber.isEmpty()) {
                resultLabel.setText("Error: Please enter IP address, port range, and thread number.");
                return;
            }
            try {
                int portRangeValue = Integer.parseInt(portRange);
                int threadNumberValue = Integer.parseInt(threadNumber);
                runPortScan(ipAddress, portRangeValue, threadNumberValue);
            } catch (NumberFormatException ex) {
                resultLabel.setText("Error: Invalid port range or thread number.");
            }
        });

        gridPane.getChildren().addAll(ipLabel, ipAddressField, portRangeLabel, portRangeField,
                threadNumberLabel, threadNumberField, scanButton, resultLabel);

        Scene scene = new Scene(gridPane, 400, 250);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public void runPortScan(String ip, int nbrPortMaxToScan, int threadNumber) {
        ExecutorService executorService = Executors.newFixedThreadPool(threadNumber);
        ConcurrentLinkedQueue<Integer> openPorts = new ConcurrentLinkedQueue<>();
        AtomicInteger port = new AtomicInteger(1);
        int timeout = 1000;
        while (port.get() <= nbrPortMaxToScan) {
            final int currentPort = port.getAndIncrement();
            executorService.submit(() -> {
                try {
                    Socket socket = new Socket();
                    socket.connect(new InetSocketAddress(ip, currentPort), timeout);
                    socket.close();
                    openPorts.add(currentPort);
                    System.out.println(ip + ", port open: " + currentPort);
                } catch (SocketTimeoutException ignored) {
                    // Ignore connection timeout errors
                } catch (IOException e) {
                    System.err.println("Error occurred while scanning port " + currentPort + ": " + e.getMessage());
                }
            });
        }
        executorService.shutdown();
        try {
            executorService.awaitTermination(10, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        List<Integer> openPortList = new ArrayList<>(openPorts);
        openPortList.forEach(p -> System.out.println("Port " + p + " is open"));
        resultLabel.setText("Open ports: " + openPortList);
    }
}

import com.github.kokorin.jaffree.ffmpeg.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

/*
 * Created by Eldevan Nery Junior on 14/09/2021 13:33
 */
public class ConverterController extends Application {

    public TableView<File> files;
    @FXML
    private Hyperlink fileName;

    @FXML
    private Button btnFile;

    @FXML
    private Label duracaoLbl;

    @FXML
    private Label tamanhoLbl;

    @FXML
    private TableColumn<File, String> arquivoColumn;

    @FXML
    private TableColumn<File, String> duracaoColumn;

    @FXML
    private TableColumn<File, String> tamanhoColumn;

    @FXML
    private TextField duracaoTextField;

    @FXML
    private TextField tamanhoTextField;

    @FXML
    private Button splitBtn;

    private FXMLLoader fxmlLoader;

    final AtomicLong durationMillis = new AtomicLong();

    @Setter
    File selectedDirectory = new File(System.getProperty("user.home"));

    private File file;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() throws Exception {
        super.init();
        this.load();

    }

    @Override
    public void start(Stage primaryStage) {
        Parent root = this.load();
        primaryStage.setTitle("SPLIT VIDEO");
        primaryStage.setResizable(true);
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    @SneakyThrows
    public Parent load() {
        ensureFxmlLoaderInitialized(getClass().getResource("converter.fxml"));
        Parent load = fxmlLoader.getRoot();
        return load;
    }
    @FXML
    private void initialize(){
        this.duracaoTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            if(StringUtils.isNotBlank(newValue)){
                Platform.runLater(()->this.tamanhoTextField.clear());
            }
        });
        this.tamanhoTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            if(StringUtils.isNotBlank(newValue)){
                this.duracaoTextField.clear();
            }
        });

        this.arquivoColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getName()));
        this.tamanhoColumn.setCellValueFactory(param -> new SimpleStringProperty(FileUtils.byteCountToDisplaySize(param.getValue().length())));
        this.duracaoColumn.setCellValueFactory(param -> {
            FFmpegResult ffmpegResult = FFmpeg.atPath()
                    .addInput(
                            UrlInput.fromPath(param.getValue().toPath())
                    )
                    .addOutput(new NullOutput())
                    .setProgressListener(new ProgressListener() {
                        @Override
                        public void onProgress(FFmpegProgress progress) {
                            durationMillis.set(progress.getTimeMillis());
                        }
                    })
                    .execute();
            String durationLbl = DurationFormatUtils.formatDuration(durationMillis.get(), "HH:mm:ss");
            return new SimpleStringProperty(durationLbl);
        });
    }

    @FXML
    void actionSelectFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Escolha o vídeo");

        chooser.setInitialDirectory(selectedDirectory);
        FileChooser.ExtensionFilter extFilter = new FileChooser
                .ExtensionFilter("Vídeos", "*.mp4", "*.avi", "*.mkv", "*.mts");
        chooser.getExtensionFilters().add(extFilter);
        chooser.getExtensionFilters().add(new FileChooser
                .ExtensionFilter("Todos os Arquivos", "*.*"));
        File fileChoosed = chooser.showOpenDialog(btnFile.getScene().getWindow());
        if (fileChoosed == null) {
            invalidaFile();
        } else {
            this.files.getItems().clear();
            this.files.refresh();
            this.file = fileChoosed;
            fileName.setText(fileChoosed.getAbsolutePath());
            FFmpegResult ffmpegResult = FFmpeg.atPath()
                    .addInput(
                            UrlInput.fromPath(file.toPath())
                    )
                    .addOutput(new NullOutput())
                    .setProgressListener(new ProgressListener() {
                        @Override
                        public void onProgress(FFmpegProgress progress) {
                            durationMillis.set(progress.getTimeMillis());
                        }
                    })
                    .execute();
            String durationLbl = DurationFormatUtils.formatDuration(durationMillis.get(), "HH:mm:ss");
            this.duracaoLbl.setText("Tempo: "  +durationLbl);
            String size = FileUtils.byteCountToDisplaySize(file.length());
            tamanhoLbl.setText("Tamanho: "+size);

        }
    }

    private void invalidaFile() {
        file = null;
        fileName.setText("Nenhum vídeo selecionado");
    }

    @FXML
    void actionSplit() {
        files.getItems().clear();
        if (file == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.initOwner(this.duracaoTextField.getScene().getWindow());
            alert.initModality(Modality.WINDOW_MODAL);
            alert.setTitle("ERRO");
            alert.setHeaderText("NENHUM ARQUIVO ESCOLHIDO!");
            alert.showAndWait();
            return;
        }
        if(StringUtils.isNotBlank(duracaoTextField.getText()) ){
            if(!StringUtils.isNumeric(duracaoTextField.getText())){
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.initOwner(this.duracaoTextField.getScene().getWindow());
                alert.initModality(Modality.WINDOW_MODAL);
                alert.setTitle("ERRO");
                alert.setHeaderText("Duração precisa ser numérica!");
                alert.showAndWait();
                return;
            }
            long toMillis = Long.valueOf(duracaoTextField.getText())*60*1000;
            System.out.println(toMillis);
            System.out.println(durationMillis.get());
            long divisao = (durationMillis.get()/toMillis);
            System.out.println(divisao);
            splitFile(toMillis, divisao + 1);

        }else if(StringUtils.isNotBlank(tamanhoTextField.getText())){
            if(!StringUtils.isNumeric(tamanhoTextField.getText())){
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.initOwner(this.duracaoTextField.getScene().getWindow());
                alert.initModality(Modality.WINDOW_MODAL);
                alert.setTitle("ERRO");
                alert.setHeaderText("Tamanho precisa ser numérico!");
                alert.showAndWait();
                return;
            }
            long bytes = Long.valueOf(tamanhoTextField.getText())*1024*1024;
            System.out.println(bytes);
            long divisao = (file.length()/bytes);
            long toMillis = (durationMillis.get()/divisao);
            splitFile(toMillis, divisao);
        }else{
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.initOwner(this.duracaoTextField.getScene().getWindow());
            alert.initModality(Modality.WINDOW_MODAL);
            alert.setTitle("ERRO");
            alert.setHeaderText("NENHUM PARAMETRO DEFINIDO(TAMANHO/DURACAO)!");
            alert.show();
            return;
        }
    }

    private void splitFile(long toMillis, long l) {
        IntStream.range(0, (int) (l)).forEach(value -> {
            String fileName = FilenameUtils.getBaseName(file.getName()) + "_" + StringUtils.leftPad((value + 1) + "", 3, "0") + "." + FilenameUtils.getExtension(file.getName());
            final AtomicLong duration = new AtomicLong();
            String durationLbl1 = DurationFormatUtils.formatDuration((value * toMillis), "HH:mm:ss");
            String durationLbl2 = DurationFormatUtils.formatDuration(toMillis, "HH:mm:ss");
            System.out.println("ffmpeg -i " + file.getName() + " -acodec copy -vcodec copy -ss " + durationLbl1 + " -t " + durationLbl2 + " " + fileName);
            String output = FilenameUtils.getFullPath(file.getPath()) + fileName;
            FFmpeg.atPath()
                    .addInput(
                            UrlInput.fromPath(file.toPath())
                    )
//                        .setFilter(StreamType.VIDEO, "scale=160:-2")
                    .setOverwriteOutput(true)
                    .addArguments("-acodec", "copy")
                    .addArguments("-vcodec", "copy")
                    .addArguments("-ss", durationLbl1)
                    .addArguments("-t", durationLbl2)
                    .addOutput(
                            UrlOutput.toUrl(output)
                    ).setProgressListener(new ProgressListener() {
                        @Override
                        public void onProgress(FFmpegProgress progress) {
                            double percents = 100. * progress.getTimeMillis() / duration.get();
                            System.out.println("Progress: " + percents + "%");
                        }
                    })
                    .execute();
            Platform.runLater(()->{
                files.getItems().add(new File(output));
            });

        });
        Platform.runLater(()->{
            this.files.refresh();
        });
    }

    protected void ensureFxmlLoaderInitialized(URL resource) {

        if (this.fxmlLoader != null) {
            return;
        }

        this.fxmlLoader = loadSynchronously(resource);
    }

    FXMLLoader loadSynchronously(URL resource) throws IllegalStateException {
        this.fxmlLoader = new FXMLLoader(resource);
        try {
            fxmlLoader.load();
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot load " + resource, ex);
        }

        return fxmlLoader;
    }



}

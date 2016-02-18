import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javafx.application.Application;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.jpedal.PdfDecoderFX;
import org.jpedal.exception.PdfException;

public class JpedalTestsFX extends Application {

    private PdfDecoderFX pdfDecoderFX = new PdfDecoderFX();

    private ImageView imageView1 = new ImageView();
    private Text jpedalInfos = new Text();
    private ImageView imageView = new ImageView();
    private Text pdfBoxInfos = new Text();

    private List<File> pdfFiles = new ArrayList<>();
    private IntegerProperty current = new SimpleIntegerProperty(-1);

    public static void main(String[] args) {
        if (args.length == 0) {
            args = new String[] { JpedalTestsFX.class.getResource("/pdfs").getFile() };
        }
        launch(args);
    }

    @Override
    public void init() throws Exception {
        getParameters().getUnnamed().forEach(s -> {
            File file = new File(s);
            if (file.isDirectory()) {
                try {
                    Files.walkFileTree(Paths.get(s), new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            if (file.toString().endsWith(".pdf")) {
                                pdfFiles.add(file.toFile());
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                if (file.toString().endsWith(".pdf")) {
                    pdfFiles.add(file);
                }
            }
            pdfFiles.add(file);
        });
    }

    @Override
    public void start(final Stage primaryStage) throws Exception {
        BorderPane borderPane = new BorderPane();
        final HBox hbox = new HBox(50, new VBox(imageView1, jpedalInfos), new VBox(imageView, pdfBoxInfos));
        hbox.setAlignment(Pos.CENTER);
        borderPane.setCenter(hbox);
        final Button leftBtn = new Button("<-");
        leftBtn.setOnAction(event -> {
            if (current.get() > 1) {
                current.set(current.get() - 1);
            }
        });
        final Button rightBtn = new Button("->");
        rightBtn.setOnAction(event -> {
            if (current.get() < pdfFiles.size()) {
                current.set(current.get() + 1);
            }
        });
        final Text fileName = new Text();
        final HBox btnBox = new HBox(10, leftBtn, fileName, rightBtn);
        btnBox.setAlignment(Pos.CENTER);
        borderPane.setTop(btnBox);
        Scene scene = new Scene(borderPane);

        current.addListener(observable -> {
            try {
                load();
                fileName.setText(pdfFiles.get(current.get()).getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            } catch (PdfException e) {
                e.printStackTrace();
            }
        });

        current.set(0);

        primaryStage.setMaximized(true);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public void load() throws IOException, PdfException {
        final File file = pdfFiles.get(current.get());

        final FileInputStream is = new FileInputStream(file);

        long beforeJpedal = System.currentTimeMillis();
        pdfDecoderFX.openPdfFileFromInputStream(is, true);
        pdfDecoderFX.decodePage(1);

        final BufferedImage jpedalImage = pdfDecoderFX.getPageAsHiRes(1);
        imageView1.setImage(SwingFXUtils.toFXImage(jpedalImage, null));
        imageView1.fitWidthProperty().set(500);
        imageView1.preserveRatioProperty().set(true);
        jpedalInfos.setText(String.valueOf(System.currentTimeMillis() - beforeJpedal) + "ms");

        long beforePdfBox = System.currentTimeMillis();
        final BufferedImage boxImage = readPDFToPNGs(new FileInputStream(file)).get(0);
        imageView.setImage(SwingFXUtils.toFXImage(boxImage, null));
        imageView.fitWidthProperty().set(500);
        imageView.preserveRatioProperty().set(true);
        pdfBoxInfos.setText(String.valueOf(System.currentTimeMillis() - beforePdfBox) + "ms");
    }

    public static List<BufferedImage> readPDFToPNGs(InputStream inputStream) throws IOException {
        java.util.List<BufferedImage> resultPNGs;
        PDDocument document = PDDocument.load(inputStream);
        PDFRenderer renderer = new PDFRenderer(document);
        resultPNGs = IntStream.range(0, 1).mapToObj(page -> {
            try {
                BufferedImage bim = renderer.renderImageWithDPI(page, 300, ImageType.RGB);
                return bim;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
        document.close();
        return resultPNGs;
    }

}

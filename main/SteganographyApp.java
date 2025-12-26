import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.nio.charset.StandardCharsets;

public class SteganographyApp extends JFrame {

    private File embedImageFile;
    private File decodeImageFile;

    private JLabel embedImagePathLabel;
    private JTextArea embedTextArea;

    private JLabel decodeImagePathLabel;
    private JTextArea decodeTextArea;

    public SteganographyApp() {
        super("Стеганография (PNG/BMP) — Java");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Встроить сообщение", createEmbedPanel());
        tabbedPane.addTab("Извлечь сообщение", createDecodePanel());

        setContentPane(tabbedPane);
    }

    private JPanel createEmbedPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Верхняя панель: выбор изображения
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        JButton chooseImageButton = new JButton("Выбрать исходное изображение (PNG/BMP)");
        embedImagePathLabel = new JLabel("Файл не выбран");
        embedImagePathLabel.setForeground(Color.DARK_GRAY);

        chooseImageButton.addActionListener(this::onChooseEmbedImage);

        topPanel.add(chooseImageButton, BorderLayout.WEST);
        topPanel.add(embedImagePathLabel, BorderLayout.CENTER);

        // Центральная часть: текст для встраивания
        embedTextArea = new JTextArea();
        embedTextArea.setLineWrap(true);
        embedTextArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(embedTextArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Текст для скрытия в изображении"));

        // Нижняя панель: кнопка "Сохранить"
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Встроить и сохранить новое изображение");
        saveButton.addActionListener(this::onSaveStegoImage);
        bottomPanel.add(saveButton);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createDecodePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Верхняя панель: выбор изображения
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        JButton chooseStegoImageButton = new JButton("Выбрать изображение с сообщением");
        decodeImagePathLabel = new JLabel("Файл не выбран");
        decodeImagePathLabel.setForeground(Color.DARK_GRAY);

        chooseStegoImageButton.addActionListener(this::onChooseDecodeImage);

        topPanel.add(chooseStegoImageButton, BorderLayout.WEST);
        topPanel.add(decodeImagePathLabel, BorderLayout.CENTER);

        // Центральная часть: извлечённый текст
        decodeTextArea = new JTextArea();
        decodeTextArea.setLineWrap(true);
        decodeTextArea.setWrapStyleWord(true);
        decodeTextArea.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(decodeTextArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Извлечённый текст"));

        // Нижняя панель: кнопка "Извлечь"
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton extractButton = new JButton("Извлечь сообщение");
        extractButton.addActionListener(this::onExtractMessage);
        bottomPanel.add(extractButton);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void onChooseEmbedImage(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            // желательно ограничить PNG/BMP, но ImageIO сам отфильтрует по расширению
            embedImageFile = file;
            embedImagePathLabel.setText(file.getAbsolutePath());
        }
    }

    private void onSaveStegoImage(ActionEvent e) {
        if (embedImageFile == null) {
            showError("Сначала выберите исходное изображение.");
            return;
        }

        String message = embedTextArea.getText();
        if (message == null || message.isEmpty()) {
            showError("Введите текст для скрытия.");
            return;
        }

        try {
            BufferedImage src = ImageIO.read(embedImageFile);
            if (src == null) {
                showError("Не удалось прочитать изображение. Поддерживаются только PNG и BMP.");
                return;
            }

            BufferedImage stego = StegUtil.encodeMessage(src, message);

            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Сохранить изображение с сообщением");
            int result = chooser.showSaveDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File outFile = chooser.getSelectedFile();

                // по умолчанию сохраняем в PNG
                String filename = outFile.getName().toLowerCase();
                String format = "png";
                if (filename.endsWith(".bmp")) {
                    format = "bmp";
                } else if (!filename.endsWith(".png")) {
                    // если пользователь не указал расширение — добавим .png
                    outFile = new File(outFile.getParentFile(), outFile.getName() + ".png");
                }

                boolean ok = ImageIO.write(stego, format, outFile);
                if (!ok) {
                    showError("Не удалось сохранить изображение (формат " + format + " не поддерживается).");
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Изображение успешно сохранено:\n" + outFile.getAbsolutePath(),
                            "Готово",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            }
        } catch (IllegalArgumentException ex) {
            showError("Ошибка стеганографии: " + ex.getMessage());
        } catch (IOException ex) {
            showError("Ошибка при работе с файлом: " + ex.getMessage());
        }
    }

    private void onChooseDecodeImage(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            decodeImageFile = file;
            decodeImagePathLabel.setText(file.getAbsolutePath());
            decodeTextArea.setText("");
        }
    }

    private void onExtractMessage(ActionEvent e) {
        if (decodeImageFile == null) {
            showError("Сначала выберите изображение с сообщением.");
            return;
        }

        try {
            BufferedImage img = ImageIO.read(decodeImageFile);
            if (img == null) {
                showError("Не удалось прочитать изображение. Поддерживаются только PNG и BMP.");
                return;
            }

            String message = StegUtil.decodeMessage(img);
            decodeTextArea.setText(message);
        } catch (IllegalArgumentException ex) {
            showError("Ошибка стеганографии: " + ex.getMessage());
        } catch (IOException ex) {
            showError("Ошибка при работе с файлом: " + ex.getMessage());
        }
    }

    private void showError(String text) {
        JOptionPane.showMessageDialog(this, text, "Ошибка", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SteganographyApp app = new SteganographyApp();
            app.setVisible(true);
        });
    }
}

/**
 * Утилита стеганографии (LSB по RGB-каналам).
 * Формат закодированных данных:
 *   [4 байта длины сообщения (big-endian)] [байты сообщения UTF-8]
 */
class StegUtil {

    /**
     * Кодирование текста в изображение.
     */
    public static BufferedImage encodeMessage(BufferedImage src, String message) {
        int width = src.getWidth();
        int height = src.getHeight();

        byte[] msgBytes = message.getBytes(StandardCharsets.UTF_8);
        int msgLen = msgBytes.length;

        long capacityBits = 1L * width * height * 3L;     // 3 канала: R,G,B
        long requiredBits = 32L + msgLen * 8L;            // 32 бита длина + данные

        if (requiredBits > capacityBits) {
            throw new IllegalArgumentException("Сообщение слишком большое для данного изображения.");
        }

        // Готовим массив данных: 4 байта длины + сообщение
        byte[] data = new byte[4 + msgLen];
        data[0] = (byte) ((msgLen >> 24) & 0xFF);
        data[1] = (byte) ((msgLen >> 16) & 0xFF);
        data[2] = (byte) ((msgLen >> 8) & 0xFF);
        data[3] = (byte) (msgLen & 0xFF);
        System.arraycopy(msgBytes, 0, data, 4, msgLen);

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        int byteIndex = 0;
        int bitIndex = 7; // от старшего к младшему

        outer:
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = src.getRGB(x, y);

                int a = (rgb >> 24) & 0xFF;
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                // Массив каналов для удобства
                int[] channels = {r, g, b};

                for (int i = 0; i < 3; i++) {
                    if (byteIndex >= data.length) {
                        // Больше нечего встраивать - записываем пиксель и всё изображение как есть
                        channels[i] = channels[i]; // без изменений
                    } else {
                        int bit = (data[byteIndex] >> bitIndex) & 1;
                        channels[i] = (channels[i] & 0xFE) | bit; // меняем только младший бит

                        bitIndex--;
                        if (bitIndex < 0) {
                            bitIndex = 7;
                            byteIndex++;
                        }
                    }
                }

                int newRgb = (a << 24) | (channels[0] << 16) | (channels[1] << 8) | channels[2];
                result.setRGB(x, y, newRgb);

                if (byteIndex >= data.length) {
                    // Остальные пиксели копируем без изменений
                    for (int yy = y; yy < height; yy++) {
                        for (int xx = (yy == y ? x + 1 : 0); xx < width; xx++) {
                            result.setRGB(xx, yy, src.getRGB(xx, yy));
                        }
                    }
                    break outer;
                }
            }
        }

        return result;
    }

    /**
     * Декодирование текста из изображения.
     */
    public static String decodeMessage(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();
        long capacityBits = 1L * width * height * 3L;

        // 1. Считываем первые 4 байта (32 бита) - длина сообщения
        byte[] lenBytes = new byte[4];
        int byteIndex = 0;
        int bitCountInByte = 0;
        int currentByte = 0;

        outerLen:
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                int[] channels = {r, g, b};

                for (int i = 0; i < 3; i++) {
                    int bit = channels[i] & 1;
                    currentByte = (currentByte << 1) | bit;
                    bitCountInByte++;

                    if (bitCountInByte == 8) {
                        lenBytes[byteIndex] = (byte) currentByte;
                        byteIndex++;
                        bitCountInByte = 0;
                        currentByte = 0;

                        if (byteIndex == 4) {
                            break outerLen;
                        }
                    }
                }
            }
        }

        if (byteIndex < 4) {
            throw new IllegalArgumentException("В изображении недостаточно данных для чтения длины сообщения.");
        }

        int msgLen = ((lenBytes[0] & 0xFF) << 24)
                | ((lenBytes[1] & 0xFF) << 16)
                | ((lenBytes[2] & 0xFF) << 8)
                | (lenBytes[3] & 0xFF);

        if (msgLen <= 0) {
            throw new IllegalArgumentException("Некорректная длина сообщения: " + msgLen);
        }

        long requiredBits = 32L + msgLen * 8L;
        if (requiredBits > capacityBits) {
            throw new IllegalArgumentException("В изображении недостаточно данных для сообщения указанной длины.");
        }

        // 2. Повторно проходим по изображению, но теперь читаем все биты и пропускаем первые 32
        byte[] msgBytes = new byte[msgLen];
        long totalBitsRead = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                int[] channels = {r, g, b};

                for (int i = 0; i < 3; i++) {
                    int bit = channels[i] & 1;

                    if (totalBitsRead >= 32) {
                        long msgBitIndex = totalBitsRead - 32; // 0.. msgLen*8 - 1
                        if (msgBitIndex < msgLen * 8L) {
                            int bytePos = (int) (msgBitIndex / 8);
                            int bitPosInByte = 7 - (int) (msgBitIndex % 8);
                            msgBytes[bytePos] |= (bit << bitPosInByte);
                        } else {
                            // Всё сообщение прочитано
                            return new String(msgBytes, StandardCharsets.UTF_8);
                        }
                    }

                    totalBitsRead++;
                }
            }
        }

        // Если вышли из циклов, но не вернули строку - пытаемся собрать то, что есть
        return new String(msgBytes, StandardCharsets.UTF_8);
    }
}

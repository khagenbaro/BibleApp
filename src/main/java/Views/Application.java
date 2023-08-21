package main.java.Views;
import main.java.Data.Bible;
import main.java.Service.DatabaseConnection;
import main.java.Service.ProcessJSON;
import main.java.Service.ProgramDirectoryService;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Vector;

public class Application extends JFrame {

    private final String path;
    private int screenWidth;
    private int screenHeight;
    private Integer x;
    private Integer y;
    private Integer textSize;
    private File selectedFile;
    private Vector<Bible> bibles;
    private JSplitPane splitPane;
    private JSplitPane verticalSplitPane;
    private JTabbedPane tabbedPane;
    private JTabbedPane notesPane;
    private JPanel emptyPanel;
    private NotesPanel notesPanel;
    private boolean isVisible = true;

    public Application() {
        // Gets Absolute Path of Application
        ProgramDirectoryService programDirectoryService = new ProgramDirectoryService();
        path = programDirectoryService.getProgramDirectory();

        // Reads config.json and sets screen size
        ProcessJSON processJSON = new ProcessJSON(new File(path + "/Resources/config.json"));
        try {
            screenWidth = processJSON.getScreenWidth();
            screenHeight = processJSON.getScreenHeight();
            x = processJSON.getX();
            y = processJSON.getY();
            isVisible = processJSON.getNavPaneVisible();
            textSize = processJSON.getTextSize();
        } catch (Exception e) {
            screenWidth = 1024;
            screenHeight = 768;
            e.printStackTrace();
        }
    }
    public boolean setUpFrame() {
        BorderLayout layout = new BorderLayout();
        layout.maximumLayoutSize(this);
        this.setTitle("Bibles");
        this.setMinimumSize(new Dimension(1024, 768));
        this.setPreferredSize(new Dimension(screenWidth,screenHeight));
        if (x == null || y == null) {
            this.setLocationRelativeTo(null);
        } else {
            this.setLocation(x, y);
        }
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setLayout(layout);
        getBibles();
        menuBar();
        toolbar();
        NavigationPane navigationPane = new NavigationPane(this);
        navigationPane.setVisible(true);
        createReaderTabbedPane();
        createNotesTabbedPane();
        verticalSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tabbedPane, notesPane);
        verticalSplitPane.setResizeWeight(0.75);
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, navigationPane, verticalSplitPane);
        if (!isVisible)
            splitPane.getLeftComponent().setVisible(false);
        this.getContentPane().add(splitPane);
        this.pack();
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                performQuit();
                e.getWindow().dispose();
                System.exit(0);
            }
        });

        Desktop desktop = Desktop.getDesktop();
        if (desktop.isSupported(Desktop.Action.APP_QUIT_HANDLER)) {
            desktop.setQuitHandler((e, response) -> {
                boolean canQuit = System.getProperty("os.name").equals("Mac OS X"); // Enables or Disables Menu Quit command on Mac OS X
                if (canQuit) {
                    performQuit();
                    response.performQuit();
                } else
                    response.cancelQuit();
            });
        }
        return true;
    }
    private void menuBar() {
        JMenuBar menu = new JMenuBar();

        // File Menu
        JMenu file = new JMenu("File");
        JMenuItem importBible = new JMenuItem("Import Bible...");
        importBible.addActionListener(e -> importDialog());
        file.add(importBible);


        if (!System.getProperty("os.name").equals("Mac OS X")) {
            file.add(new JSeparator());
            JMenuItem quit = new JMenuItem("Quit");
            quit.addActionListener(e -> {
                performQuit();
                this.setVisible(false);
                this.dispose();
                System.exit(0);
            });
            file.add(quit);
        }

        menu.add(file);

        // View Menu
        JMenu view = new JMenu("View");
        JMenuItem addBible = new JMenuItem("Open new Bible");
        addBible.addActionListener(event -> addBibleDialog());
        JMenuItem navView = new JMenuItem("Show/Hide Nav Pane");
        navView.addActionListener(e -> {
            if (splitPane.getLeftComponent().isVisible()) {
                splitPane.getLeftComponent().setVisible(false);
            } else {
                splitPane.getLeftComponent().setVisible(true);
                splitPane.setDividerLocation(200);
            }
        });

        view.add(addBible);
        view.addSeparator();
        view.add(navView);

        menu.add(view);

        this.setJMenuBar(menu);
        this.add(menu);

    }

    private void toolbar() {
        JToolBar toolBar = new JToolBar();

        // Show/Hide Navigation Pane
        JButton showNavPane = new JButton();
        showNavPane.setIcon(new ImageIcon(path + "/Resources/Icons/sidePanel.png"));
        showNavPane.setPreferredSize(new Dimension(50, 50));
        showNavPane.addActionListener(e -> {
            if (splitPane.getLeftComponent().isVisible()) {
                splitPane.getLeftComponent().setVisible(false);
            } else {
                splitPane.getLeftComponent().setVisible(true);
                splitPane.setDividerLocation(200);
            }
        });
        showNavPane.setToolTipText("Show/Hide Navigation Pane");
        toolBar.add(showNavPane);

        // Add Separator
        toolBar.addSeparator();

        // Open new book
        JButton openBook = new JButton();
        openBook.setIcon(new ImageIcon(path + "/Resources/Icons/openBible.png"));
        openBook.setToolTipText("Open a Bible");
        openBook.setPreferredSize(new Dimension(50, 50));
        openBook.addActionListener(event -> addBibleDialog());

        toolBar.add(openBook);

        // Import Button
        JButton importBook = new JButton();
        importBook.setIcon(new ImageIcon(path + "/Resources/Icons/import.png"));
        importBook.setPreferredSize(new Dimension(50, 50));
        importBook.setToolTipText("Import a new Bible");
        importBook.addActionListener(event -> importDialog());

        toolBar.add(importBook);
        toolBar.setRollover(true);
        toolBar.setFloatable(false);
        this.getContentPane().add(toolBar, BorderLayout.NORTH);
    }

    private void createReaderTabbedPane() {
        tabbedPane = new JTabbedPane();
        tabbedPane.addChangeListener(event -> {
            ReaderPanel temp = (ReaderPanel) tabbedPane.getSelectedComponent();
            if (notesPanel != null) {
                updateNotes(temp.getBible(), temp.getBook(), temp.getChapter());
            }
        });

        // Get Saved tabs and restore them
        ArrayList<Long[]> tabs = new ArrayList<>();
        ProcessJSON processJSON = new ProcessJSON(new File(path + "/Resources/config.json"));
        try {
            tabs = processJSON.getTabs();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Restore tabs
        if (tabs.isEmpty()) {
            addReaderPane(bibles.get(0), 1L, 1L);
        } else {
            for (Long[] tab : tabs) {
                Bible selected = new Bible();
                for (Bible bible : bibles) {
                    if (Objects.equals(bible.getBibleId(), tab[0])) {
                        selected = bible;
                    }
                }
                addReaderPane(selected, tab[1], tab[2]);
                if (tab[3] == 1) {
                    tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
                }
            }
        }
    }

    private void addReaderPane(Bible bible, Long bookId, Long chapterId) {
        int index = tabbedPane.getTabCount();
        ReaderPanel readerPanel = new ReaderPanel(this, textSize, bible.getBibleId(), bookId, chapterId);

        // Creates Tab Label and close button
        JPanel tabPanel = new JPanel();
        tabPanel.setOpaque(false);
        tabPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JLabel label = new JLabel(bible.getBibleShortName());
        JButton closeButton = new JButton();
        closeButton.setIcon(new ImageIcon(path + "/Resources/Icons/close.png"));
        closeButton.setPreferredSize(new Dimension(18, 18));
        closeButton.setToolTipText("Close Tab");
        closeButton.addActionListener(event -> {
            tabbedPane.removeTabAt(tabbedPane.indexOfTabComponent(tabPanel));
            if (tabbedPane.getTabCount() == 0) {
                emptyTabbedPane();
            }
        });
        tabPanel.add(closeButton);
        tabPanel.add(label);

        // Adds Panel to the tabbed pane
        tabbedPane.insertTab(bible.getBibleShortName(), null, readerPanel, bible.getBibleName(), index);
        tabbedPane.setTabComponentAt(index, tabPanel);
    }

    private void emptyTabbedPane() {
        emptyPanel = new JPanel();
        emptyPanel.setLayout(new BorderLayout());

        // Empty message
        JLabel label = new JLabel("Nothing to display");
        label.setHorizontalAlignment(JLabel.CENTER);
        emptyPanel.add(label, BorderLayout.CENTER);
        // Sets tabbed pane to not show and displays empty message
        tabbedPane.setVisible(false);
        splitPane.remove(tabbedPane);
        splitPane.add(emptyPanel);
    }

    private void createNotesTabbedPane() {

        notesPane = new JTabbedPane();
        createNotesTab();

    }

    private void createNotesTab() {
        ReaderPanel readerPanel = (ReaderPanel) tabbedPane.getSelectedComponent();
        notesPanel = new NotesPanel(this, textSize, readerPanel.getBible(), readerPanel.getBook(), readerPanel.getChapter());

        notesPane.addTab("Notes", notesPanel);
    }

    public void updateNotes(Long bibleId, Long bookId, Long chapterId) {
        notesPanel.updateNotes(bibleId, bookId, chapterId);
    }

    private void importDialog() {
        JDialog jDialog = new JDialog(this, "Import Bible");
        jDialog.setLayout(new BorderLayout());
        jDialog.setSize(400, 100);
        jDialog.setLocation((this.getWidth() / 2) + this.getX() - 200, (this.getHeight() / 2) + this.getY() - 50);
        createSelectFilePane(jDialog);
        jDialog.setVisible(true);
    }

    private void createSelectFilePane(JDialog dialog) {
        JButton uploadButton = new JButton("Upload");
        // File Chooser pop-up
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("json", "json"));
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home") +
                System.getProperty("file.separator") + "BibleApp"));

        // Panel for selection of file
        JPanel fileSelectPanel = new JPanel();
        JButton button = new JButton("Select File");
        JLabel label = new JLabel();
        label.setText("No file is selected");
        button.addActionListener(event -> {
            int result = fileChooser.showDialog(this, "Select");
            if (result == JFileChooser.APPROVE_OPTION) {
                selectedFile = fileChooser.getSelectedFile();
                label.setText(selectedFile.getName());
                uploadButton.setEnabled(true);
            }
        });
        fileSelectPanel.add(button);
        fileSelectPanel.add(label);

        // panel for Buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BorderLayout());
        buttonPanel.setBorder(new EmptyBorder(10,10,10,10));
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(event -> {
            dialog.setVisible(false);
            dialog.dispose();
        });
        uploadButton.setEnabled(false);
        uploadButton.setOpaque(true);
        uploadButton.setBorderPainted(false);
        uploadButton.setBackground(Color.BLUE);
        uploadButton.setForeground(Color.WHITE);
        uploadButton.addActionListener(event -> {
            processUpload();
            dialog.setVisible(false);
            dialog.dispose();
        });
        buttonPanel.add(cancelButton, BorderLayout.WEST);
        buttonPanel.add(uploadButton, BorderLayout.EAST);

        dialog.getContentPane().add(fileSelectPanel, BorderLayout.NORTH);
        dialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
    }

    private void processUpload() {
        //  TO DO: Add logic to process uploaded file
        ProcessJSON processJSON = new ProcessJSON(selectedFile);
        try {
            int complete = processJSON.saveBibleToDatabase(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        getBibles();
    }

    public void navigateTo(Long book, Long chapter) {
        // If no tabs exist, Create one with the first Bible in the list
        if (tabbedPane.getTabCount() == 0) {
            addReaderPane(bibles.get(0), 1L, 1L);
            tabbedPane.setVisible(true);
            splitPane.remove(emptyPanel);
            splitPane.add(tabbedPane);
            updateNotes(bibles.get(0).getBibleId(), 1L, 1L);
        }

        ReaderPanel readerPanel = (ReaderPanel) tabbedPane.getSelectedComponent();
        readerPanel.setSearchFields(readerPanel.getBible(), book, chapter);
        updateNotes(readerPanel.getBible(), book, chapter);
    }

    public void setTextSize(int textSize) {
        this.textSize = textSize;
        notesPanel.setTextSize(textSize);
    }

    public void getBibles() {
        try {
            DatabaseConnection connection = new DatabaseConnection();
            bibles = connection.getBibles();
            connection.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error connecting to database", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addBibleDialog() {
        JDialog dialog = new JDialog(this, "Select a Bible");
        dialog.setLayout(new BorderLayout());
        dialog.setResizable(false);
        dialog.setSize(new Dimension(400, 150));
        dialog.setLocation((this.getWidth() / 2) + this.getX() - 200, (this.getHeight() / 2) + this.getY() - 75);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        // Make Labels
        JPanel labelPanel = new JPanel();
        setLayout(new BorderLayout());
        JLabel icon = new JLabel(new ImageIcon(path + "/Resources/Icons/openBible.png"));
        JLabel label = new JLabel("Please select a Bible");
        label.setFont(new Font("Serif", Font.PLAIN, 18));
        labelPanel.add(icon);
        labelPanel.add(label);

        // Drop Down
        JPanel dropDownPanel = new JPanel();
        JComboBox<Object> comboBox = new JComboBox<>();
        comboBox.addItem("-- Select a Bible --");
        for (Bible bible : bibles) {
            comboBox.addItem(bible.getBibleName());
        }
        dropDownPanel.add(comboBox);

        // Buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BorderLayout());
        buttonPanel.setBorder(new EmptyBorder(10,10,10,10));
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            dialog.setVisible(false);
            dialog.dispose();
        });
        JButton submitButton = new JButton("Submit");
        submitButton.setBackground(Color.BLUE);
        submitButton.setForeground(Color.WHITE);
        submitButton.addActionListener(e -> {
            for (Bible bible : bibles) {
                if (Objects.equals(Objects.requireNonNull(comboBox.getSelectedItem()).toString(), bible.getBibleName())) {
                    addReaderPane(bible, 1L, 1L);
                }
            }
            if (tabbedPane.getTabCount() == 1) {
                tabbedPane.setVisible(true);
                splitPane.remove(emptyPanel);
                splitPane.add(tabbedPane);
            }
            tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
            dialog.setVisible(false);
            dialog.dispose();
        });
        buttonPanel.add(cancelButton, BorderLayout.WEST);
        buttonPanel.add(submitButton, BorderLayout.EAST);

        panel.add(labelPanel, BorderLayout.NORTH);
        panel.add(dropDownPanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void performQuit() {
        // Get open tabs to save to config.json
        ArrayList<Long[]> openTabs = new ArrayList<>();
        long isSelected;
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            ReaderPanel readerPanel = (ReaderPanel) tabbedPane.getComponentAt(i);
            if (tabbedPane.getSelectedIndex() == i)
                isSelected = 1L;
            else
                isSelected = 0L;
            Long[] tab = {
                    readerPanel.getBible(),
                    readerPanel.getBook(),
                    readerPanel.getChapter(),
                    isSelected

            };
            openTabs.add(tab);
        }

        ProcessJSON processJSON = new ProcessJSON(new File(path + "/Resources/config.json"));
        try {
            processJSON.setScreenHeight(this.getWidth(), this.getHeight());
            processJSON.setWindowPosition(this.getX(), this.getY());
            processJSON.saveTabs(openTabs);
            processJSON.setNavPaneVisible(splitPane.getLeftComponent().isVisible());
            processJSON.setTextSize(textSize);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

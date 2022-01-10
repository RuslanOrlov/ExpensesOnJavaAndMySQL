/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package expensesdb.expensesdb;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.text.MaskFormatter;

/**
 *
 * @author Ruslan G. Orlov
 */
public class MainClass {
    public static final int ADD_MODE = 0;
    public static final int EDIT_MODE = 1;
    public static final int VIEW_MODE = 2;
    
    Properties          props, guiTitles;
    Connection          conn;
    String              typeOperation;
    ResultSet           crs;
    ResultSet           crsDetails;
    
    JFrame              frame;
    JPanel              background;
    JMenuBar            menuBar;
    JTable              table;
    AbstractTableModel  myModel;
    JPanel              backgroundOut;
    
    final static String[] quantityArr   = new String[] {"не выбрано", "шт", 
                                                        "ед", "экз"};
    final static String[] sizesArr      = new String[] {"не выбрано", "кг", 
                                                        "гр", "литр", "мл", 
                                                        "метр", "см", "кв.м"};
    
    JTextField          numField;
    JFormattedTextField dateField;
    JTextField          sumField;
    JTextField          kindField;
    JTextField          orgOrPersonField;
    JComboBox<String>   orgOrPersonTypeField;
    JTextArea           descriptionField;
    
    public MainClass() {
        super();
        guiTitles = new Properties();
        guiTitles.put("CardExpense", "Записать расходы");
        guiTitles.put("CardIncome", "Записать доходы");
        guiTitles.put("CardAsset", "Записать активы");
        guiTitles.put("ListExpenses", "Просмотреть расходы");
        guiTitles.put("ListIncomes", "Просмотреть доходы");
        guiTitles.put("ListAssets", "Просмотреть активы");
        guiTitles.put("ViewBalance", "Просмотреть баланс");
    }
    
    /*
     * Далее расположен стартовый и инициализирующий код приложения. 
     */
    
    public static void main(String[] args) {
        MainClass mс = new MainClass();
        mс.go();
    }
    
    void go() {
        readPropertiesFile();
        setConnectionDB();
        EventQueue.invokeLater(() -> {
            buildApplicationGUI();
        });
    }
    
    void readPropertiesFile() {
        File propsFile = new File("database.properties");
        if (!propsFile.exists()) {
            JOptionPane.showMessageDialog(
                    null, "Отсутствует  файл  свойств  базы  данных !    "
                      + "\nСоздайте   этот   файл    или    обратитесь  к"
                      + "\nадминистратору для разрешения ситуации,"
                      + "\nпосле чего запустите приложение еще раз !", 
                    "Отсутствует файл свойств БД", 
                    JOptionPane.WARNING_MESSAGE);
            System.exit(0);
        } else {
            props = new Properties();
            try (FileInputStream fis = new FileInputStream(propsFile)) {
                props.load(fis);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(MainClass.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(MainClass.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    void setConnectionDB() {
        String drivers  = props.getProperty("jdbc.drivers");
        if (drivers != null) System.setProperty("jdbc.drivers", drivers);
        String url      = props.getProperty("jdbc.url");
        String username = props.getProperty("jdbc.username");
        String password = props.getProperty("jdbc.password");
        try {
            conn = DriverManager.getConnection(url, username, password);
        } catch (SQLException ex) {
            Logger.getLogger(MainClass.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /*
     * Далее расположен код, который отвечает за отрисовку ГПИ самого верхнего 
     * уровня приложения и логику вызова уровней ГПИ приложения, отвечающих за 
     * формирование БД расходов, доходов, активов и отображение баланса. 
     */
    
    public void buildApplicationGUI() {
        frame = new JFrame("Расходы и Доходы");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing​(WindowEvent e) {
                if (conn != null) 
                    try {
                        conn.close();
                        System.out.println("Выполнена команда - conn.close();");
                    } catch (SQLException ex) {
                        Logger.getLogger(MainClass.class.getName()).log(Level.SEVERE, null, ex);
                    }
            }
        });
        
        background = new JPanel(new BorderLayout());
        background.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        MyInputListener inputListener = new MyInputListener();
        MyOutputListener outputListener = new MyOutputListener();
        MyViewBalanceListener viewBalanceListener = new MyViewBalanceListener();
        
        menuBar = new JMenuBar();
        
        JMenu addMenu = new JMenu("Добавить");
        JMenu viewMenu = new JMenu("Просмотреть");
        
        JMenuItem addExpenseItem = new JMenuItem("Запись о расходах");
        JMenuItem addIncomeItem = new JMenuItem("Запись о доходах");
        JMenuItem addAssetItem = new JMenuItem("Запись об активах");
        JMenuItem viewExpensesItem = new JMenuItem("Список расходов");
        JMenuItem viewIncomesItem = new JMenuItem("Список доходов");
        JMenuItem viewAssetsItem = new JMenuItem("Список активов");
        JMenuItem viewBalanceItem = new JMenuItem("Баланс");
        
        addExpenseItem.addActionListener(inputListener);
        addIncomeItem.addActionListener(inputListener);
        addAssetItem.addActionListener(inputListener);
        viewExpensesItem.addActionListener(outputListener);
        viewIncomesItem.addActionListener(outputListener);
        viewAssetsItem.addActionListener(outputListener);
        viewBalanceItem.addActionListener(viewBalanceListener);
        
        addExpenseItem.setActionCommand("CardExpense");
        addIncomeItem.setActionCommand("CardIncome");
        addAssetItem.setActionCommand("CardAsset");
        viewExpensesItem.setActionCommand("ListExpenses");
        viewIncomesItem.setActionCommand("ListIncomes");
        viewAssetsItem.setActionCommand("ListAssets");
        viewBalanceItem.setActionCommand("ViewBalance");
        
        addMenu.add(addExpenseItem);
        addMenu.add(addIncomeItem);
        addMenu.add(addAssetItem);
        viewMenu.add(viewExpensesItem);
        viewMenu.add(viewIncomesItem);
        viewMenu.add(viewAssetsItem);
        viewMenu.addSeparator();
        viewMenu.add(viewBalanceItem);
        menuBar.add(addMenu);
        menuBar.add(viewMenu);
        
        JPanel panelIn = new JPanel();
        panelIn.setLayout(new BoxLayout(panelIn, BoxLayout.PAGE_AXIS));
        panelIn.setBorder(BorderFactory.createTitledBorder(
                                    BorderFactory.createLineBorder(Color.GRAY), 
                                    "Новые операции"));
        
        JPanel panelOut = new JPanel();
        panelOut.setLayout(new BoxLayout(panelOut, BoxLayout.PAGE_AXIS));
        panelOut.setBorder(BorderFactory.createTitledBorder(
                                    BorderFactory.createLineBorder(Color.GRAY),
                                    "Сводные данные", TitledBorder.RIGHT, TitledBorder.TOP));
        
        JPanel panelBalance = new JPanel();
        panelBalance.setLayout(new BoxLayout(panelBalance, BoxLayout.PAGE_AXIS));
        panelBalance.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        
        JButton buttonINExpenses    = new JButton("Записать расходы");
        JButton buttonINIncomes     = new JButton("Записать доходы");
        JButton buttonINAssets      = new JButton("Записать активы");
        JButton buttonListExpenses  = new JButton("Просмотреть расходы");
        JButton buttonListIncome    = new JButton("Просмотреть доходы");
        JButton buttonListAssets    = new JButton("Просмотреть активы");
        JButton buttonViewBalance   = new JButton("Просмотреть баланс");
        
        buttonINExpenses.addActionListener(inputListener);
        buttonINIncomes.addActionListener(inputListener);
        buttonINAssets.addActionListener(inputListener);
        buttonListExpenses.addActionListener(outputListener);
        buttonListIncome.addActionListener(outputListener);
        buttonListAssets.addActionListener(outputListener);
        buttonViewBalance.addActionListener(viewBalanceListener);
        
        buttonINExpenses.setActionCommand("CardExpense");
        buttonINIncomes.setActionCommand("CardIncome");
        buttonINAssets.setActionCommand("CardAsset");
        buttonListExpenses.setActionCommand("ListExpenses");
        buttonListIncome.setActionCommand("ListIncomes");
        buttonListAssets.setActionCommand("ListAssets");
        buttonViewBalance.setActionCommand("ViewBalance");
        
        buttonListExpenses.setAlignmentX(Component.RIGHT_ALIGNMENT);
        buttonListIncome.setAlignmentX(Component.RIGHT_ALIGNMENT);
        buttonListAssets.setAlignmentX(Component.RIGHT_ALIGNMENT);
        buttonViewBalance.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        panelIn.add(Box.createRigidArea(new Dimension(0, 7)));
        panelIn.add(buttonINExpenses);
        panelIn.add(Box.createRigidArea(new Dimension(0, 7)));
        panelIn.add(buttonINIncomes);
        panelIn.add(Box.createRigidArea(new Dimension(0, 7)));
        panelIn.add(buttonINAssets);
        panelIn.add(Box.createRigidArea(new Dimension(0, 7)));
        
        panelOut.add(Box.createRigidArea(new Dimension(0, 7)));
        panelOut.add(buttonListExpenses);
        panelOut.add(Box.createRigidArea(new Dimension(0, 7)));
        panelOut.add(buttonListIncome);
        panelOut.add(Box.createRigidArea(new Dimension(0, 7)));
        panelOut.add(buttonListAssets);
        panelOut.add(Box.createRigidArea(new Dimension(0, 7)));
        
        panelBalance.add(Box.createRigidArea(new Dimension(0, 10)));
        panelBalance.add(buttonViewBalance);
        panelBalance.add(Box.createRigidArea(new Dimension(0, 10)));
        
        background.add(BorderLayout.WEST, panelIn);
        background.add(BorderLayout.EAST, panelOut);
        background.add(BorderLayout.SOUTH, panelBalance);
        
        drawApplicationGui(background, menuBar, 300, 300, true, true);
    }
    
    void drawApplicationGui(JPanel panel, JMenuBar menuBar, 
                            int width, int height, 
                            boolean isPack, boolean isCentered) {
        frame.setContentPane(panel);
        frame.setJMenuBar(menuBar);
        frame.setSize(width, height);
        if (isPack) 
            frame.pack();
        if (isCentered) 
            frame.setLocationRelativeTo(null);  // Центрирование окна на экране
        frame.setVisible(true);
    }
    
    class MyInputListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent a) {
            typeOperation = a.getActionCommand();
            frame.setJMenuBar(null);
            buildGuiCard(0, ADD_MODE, 0L);
        }
    }
    
    class MyOutputListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent a) {
            typeOperation = a.getActionCommand();
            frame.setJMenuBar(null);
            buildGuiOut();
        }
    }
    
    class MyViewBalanceListener implements  ActionListener {
        @Override
        public void actionPerformed(ActionEvent a) {
            typeOperation = a.getActionCommand();
            frame.setJMenuBar(null);
            buildGuiBalance();
        }
    }
    
    /*
     * Далее расположен код, который отвечает за отрисовку ГПИ и логику 
     * добавления записей в спсики расходов, доходов, активов.
     */
    
    public void buildGuiCard(int rowIndex, int mode, long rowIdValue) {
        int numRows, numCols, width, height, columnsForButton;
        
        // Вычисляем количество колонок для кнопок на интерфейсной форме, 
        // в зависимости от типа операции (typeOperation) и режима (mode)
        if ( (typeOperation.equals("CardExpense") || 
              typeOperation.equals("CardAsset")) && mode == ADD_MODE 
                || 
             (typeOperation.equals("ListExpenses") || 
              typeOperation.equals("ListAssets")) && mode == EDIT_MODE ) 
            columnsForButton = 3;
        else columnsForButton = 2;
        
        if ( (typeOperation.equals("ListExpenses") || 
              typeOperation.equals("ListAssets")) && mode == VIEW_MODE ) 
             columnsForButton = 2;
        else columnsForButton = 3;
        
        if (mode == ADD_MODE) numRows = 6;
        else                  numRows = 7;
        numCols = 2; width  = 510; height = 250;
        
        // Создаем необходимые панели для использования в качестве: 
        // - контейнера     (backgroundIn)
        // - карточного ГПИ (cardPanel)
        // - кнопок         (buttonsPanel)
        JPanel backgroundIn = new JPanel(new BorderLayout());
        Border emptyBorder = BorderFactory.createEmptyBorder(10, 10, 5, 10);
        backgroundIn.setBorder(BorderFactory.createTitledBorder(emptyBorder, 
                                        guiTitles.getProperty(typeOperation)));
        
        JPanel cardPanel = new JPanel(new GridLayout(numRows, numCols, 5, 3));
        
        JPanel buttonsPanel = new JPanel(new GridLayout(1, columnsForButton, 20, 0));
        buttonsPanel.setBorder(BorderFactory.createEmptyBorder(10, 9, 5, 9));
        
        // Создаем метки карточной интерфейсной формы. Метка numLabel НЕ исполь-
        // пользуется в ГПИ, если текущий режим (mode) РАВЕН значению ADD_MODE
        JLabel numLabel                 = new JLabel("№ п/п:");
        JLabel dateLabel                = new JLabel("Дата:");
        JLabel sumLabel                 = new JLabel("Сумма:");
        JLabel kindLabel                = new JLabel("Вид операции:");
        JLabel orgsOrPersonTypeLabel    = new JLabel("Тип корреспондента:");
        JLabel orgsOrPersonLabel        = new JLabel("Организация/лицо:");
        JLabel descriptionLabel         = new JLabel("Описание:");
        
        // Создаем поля карточной интерфейсной формы. Поле numField НЕ исполь-
        // зуется в ГПИ, если текущий режим (mode) РАВЕН значению ADD_MODE
        numField          = new JTextField(5);
        // Установить формат даты и инициализировать поле даты текущей датой
        MaskFormatter formatter = null;
        try {
            formatter     = new MaskFormatter("##-##-####");
            formatter.setPlaceholderCharacter(' ');
        } catch (ParseException ex) {
            Logger.getLogger(MainClass.class.getName()).log(Level.SEVERE, null, ex);
        }
        dateField         = new JFormattedTextField(formatter);
        if (mode == ADD_MODE) 
            dateField.setText(String.format("%td-%<tm-%<tY", new Date()));
        sumField          = new JTextField("", 10);
        kindField         = new JTextField(
                                    guiTitles.getProperty(typeOperation), 10);
        orgOrPersonTypeField    = new JComboBox<>(
                                    new String[] {"Лицо", "Организация"});
        if (mode == ADD_MODE) 
            orgOrPersonTypeField.setSelectedItem("Лицо");
        orgOrPersonField  = new JTextField("", 10);
        descriptionField  = new JTextArea("", 3, 10); 
        descriptionField.setLineWrap(true);
        
        JScrollPane scroller = new JScrollPane(descriptionField, 
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        
        // В зависимости от значения текущего режима (mode), поля карточной 
        // интерфейсной формы разрешаются / запрещаются для редактирования
        boolean isEditableCard = true;
        if (mode == VIEW_MODE) isEditableCard = false;
        numField.setEditable(false);
        dateField.setEditable(isEditableCard);
        sumField.setEditable(isEditableCard);
        kindField.setEditable(isEditableCard);
        orgOrPersonTypeField.setEditable(isEditableCard);
        orgOrPersonField.setEditable(isEditableCard);
        descriptionField.setEditable(isEditableCard);
        
        // Если текущий режим (mode) НЕ РАВЕН значению ADD_MODE, поля 
        // карточной интерфейсной формы инициализируются значениями
        // полей выделенной записи таблицы списочной интерфейсной формы
        if (mode != ADD_MODE) {
            int columnIndex = 0;
            numField        .setText(Long.toString((Long)myModel
                            .getValueAt(rowIndex, table.convertColumnIndexToModel(columnIndex++))));
            dateField       .setText(String.format("%td-%<tm-%<tY", (Date)myModel
                            .getValueAt(rowIndex, table.convertColumnIndexToModel(columnIndex++)))); 
            sumField        .setText( ((BigDecimal)myModel
                            .getValueAt(rowIndex, table.convertColumnIndexToModel(columnIndex++))).toString() );
            kindField       .setText((String)myModel
                            .getValueAt(rowIndex, table.convertColumnIndexToModel(columnIndex++)));
            orgOrPersonTypeField.setSelectedItem( ((Number)myModel.
                            getValueAt(rowIndex, 7)).longValue() == 1L ? "Лицо" : "Организация");
            orgOrPersonField.setText((String)myModel
                            .getValueAt(rowIndex, table.convertColumnIndexToModel(columnIndex++)));
            descriptionField.setText((String)myModel
                            .getValueAt(rowIndex, table.convertColumnIndexToModel(columnIndex++)));
        }
        
        // Размещаем метки и поля в карточной интерфейсной форме. 
        // Метка numLabel и поле numField НЕ испольпользуются в 
        // ГПИ, если текущий режим (mode) РАВЕН значению ADD_MODE
        if (mode != ADD_MODE) {
            cardPanel.add(numLabel);            cardPanel.add(numField);
        }
        cardPanel.add(dateLabel);               cardPanel.add(dateField);
        cardPanel.add(sumLabel);                cardPanel.add(sumField);
        cardPanel.add(kindLabel);               cardPanel.add(kindField);
        cardPanel.add(orgsOrPersonTypeLabel);   cardPanel.add(orgOrPersonTypeField);
        cardPanel.add(orgsOrPersonLabel);       cardPanel.add(orgOrPersonField);
        cardPanel.add(descriptionLabel);        cardPanel.add(scroller);
        
        // Создаем кнопки, регистрируем в них прослущивателей 
        // событий, после чего добавляем на панель кнопок
        JButton saveButton    = new JButton("Сохранить");
        JButton detailsButton = new JButton("Детали");
        JButton cancelButton  = new JButton("Отменить");
        switch(mode) {
            case (ADD_MODE):
                saveButton.addActionListener(new MyAddDataListener());
                cancelButton.addActionListener(event -> drawApplicationGui(
                                background, menuBar, 300, 300, true, true));
                buttonsPanel.add(saveButton);
                if (typeOperation.equals("CardExpense") || 
                    typeOperation.equals("CardAsset")) {
                    detailsButton.addActionListener(event -> buildDetailsGui(
                        backgroundIn, null, width, height, false, false, mode, rowIdValue));
                    buttonsPanel.add(detailsButton);
                }
                buttonsPanel.add(cancelButton);
                break;
            case (EDIT_MODE):
                saveButton.addActionListener(new MyChangeDataListener(rowIndex));
                cancelButton.addActionListener(event -> drawApplicationGui(
                                backgroundOut, null, 590, 300, false, true));
                buttonsPanel.add(saveButton);
                if (typeOperation.equals("ListExpenses") || 
                        typeOperation.equals("ListAssets")) {
                    detailsButton.addActionListener(event -> buildDetailsGui(
                        backgroundIn, null, width, height, false, false, mode, rowIdValue));
                    buttonsPanel.add(detailsButton);
                }
                buttonsPanel.add(cancelButton);
                break;
            case (VIEW_MODE): 
                cancelButton.setText("Обратно к списку");
                cancelButton.addActionListener(event -> drawApplicationGui(
                                backgroundOut, null, 590, 300, false, true));
                if (typeOperation.equals("ListExpenses") || 
                        typeOperation.equals("ListAssets")) {
                    detailsButton.addActionListener(event -> buildDetailsGui(
                        backgroundIn, null, width, height, false, false, mode, rowIdValue));
                    buttonsPanel.add(detailsButton);
                    buttonsPanel.add(cancelButton);
                } else {
                    buttonsPanel.add(new JLabel());
                    buttonsPanel.add(cancelButton);
                    buttonsPanel.add(new JLabel());
                }
                break;
        }
        backgroundIn.add(BorderLayout.CENTER, cardPanel);
        backgroundIn.add(BorderLayout.SOUTH, buttonsPanel);
        
        drawApplicationGui(backgroundIn, null, width, height, false, false);
    }
    
    class MyAddDataListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent a) {
            // Выполняем  проверку на кооректность и полноту  заполнения 
            // полей интерфейсной формы. Если проверка прошла неуспешно, 
            // выходим  из метода, отобразив сообщение с предупреждением
            String message;
            if (!"".equals(message = checkCorrectInput())) {
                JOptionPane.showMessageDialog(frame, message,
                        "Предупреждение", JOptionPane.WARNING_MESSAGE);
                return;
            }
            // Извлекаем  значения полей из интерфейса добавления новой записи и 
            // вставляем новую запись в результирующий набор "crs", а также в БД
            addRecordDB();
            // Отрисовываем главный интерфейс приложения, 
            // возвращаясь  на верхний уровень программы
            drawApplicationGui(background, menuBar, 300, 300, true, true);
        }
    }
    
    class MyChangeDataListener implements ActionListener {
        int rowIndex;
        public MyChangeDataListener(int rowIndex) {
            this.rowIndex = rowIndex;
        }
        
        @Override
        public void actionPerformed(ActionEvent a) {
            // Выполняем проверку на кооректность и полноту заполнения 
            // полей интерфейсной формы. Если проверка прошла неуспешно, 
            // выходим из метода, отобразив сообщение с предупреждением
            String message;
            if (!"".equals(message = checkCorrectInput())) {
                JOptionPane.showMessageDialog(frame, message,
                        "Предупреждение", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Извлекаем значения полей из интерфейса редактирования записи 
            // и обновляем нашу запись в "crs" (ResultSet), а также в БД 
            changeRecordDB();
            
            // Отрисовываем интерфейс приложения, отображающий список 
            // записей, возвращаясь на предыдущий уровень программы
            drawApplicationGui(backgroundOut, null, 590, 300, false, true);
            myModel.fireTableDataChanged();
        }
    }
    
    public boolean checkDate(String date) {
        // Если дата меньше 10 символов, то вернуть false.
        if (date.length() < 10) return false;
        
        // Иначе "разобрать" дату на части и подготовить списки 
        // для проверки корректности количества дней в месяцах.
        String[] dayMonth = date.split("-");
        ArrayList<Integer> monthOf31Days = new ArrayList<>();
            monthOf31Days.add(1);   monthOf31Days.add(3);
            monthOf31Days.add(5);   monthOf31Days.add(7);
            monthOf31Days.add(8);   monthOf31Days.add(10);
            monthOf31Days.add(12);
        ArrayList<Integer> monthOf30Days = new ArrayList<>();
            monthOf30Days.add(4);   monthOf30Days.add(6);
            monthOf30Days.add(9);   monthOf30Days.add(11);
        
        // Проверить день и месяц даты на "пустое" значение.
        // Если проверки прошли неуспешно, то вернуть false.
        // Данная проверка является избыточной.
        if (dayMonth.length >= 2 && ( dayMonth[0].equals("") || 
                                      dayMonth[0].equals("  ") ||
                                      dayMonth[1].equals("") || 
                                      dayMonth[1].equals("  ") 
                                    ) 
            ) return false;
        
        // Проверить каждый элемент даты (день, месяц, год).
        // Если проверки прошли неуспешно, то вернуть false.
        if (dayMonth.length == 3) {
            int day     = Integer.parseInt(dayMonth[0]);
            int month   = Integer.parseInt(dayMonth[1]);
            int year    = Integer.parseInt(dayMonth[2]);
            if ( monthOf31Days.contains(month) && (day < 1 || day > 31) || 
                 monthOf30Days.contains(month) && (day < 1 || day > 30) || 
                 ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)) 
                                 && month == 2 && (day < 1 || day > 29) ||
                 (year % 4 != 0) && month == 2 && (day < 1 || day > 28) ||
                 (month < 1 || month > 12)  || 
                 year < 2000 || 
                 year > Integer.parseInt(String.format("%tY", new Date())) 
               ) return false;
        }
        
        // Если мы здесь, значит все проверки успешны, и нужно вернуть true.
        return true;
    }
    
    public String checkCorrectInput() {
        String message = "";
        if (!checkDate(dateField.getText().trim())) message += "\n  - 'Дата'";
        if (sumField.getText().trim().equals("")) message += "\n  - 'Сумма'";
        if (kindField.getText().trim().equals("")) message += "\n  - 'Вид операции'";
        if (orgOrPersonField.getText().trim().equals("")) message += "\n  - 'Организация/лицо'";
        if (descriptionField.getText().trim().equals("")) message += "\n  - 'Описание'";
        if (!message.equals("")) message = 
                "Не определены значения ниже следующих полей !!!" + message
              + "\nОбязательно заполните значения перечисленных полей "
              + "\nдля продолжения работы или нажмите кнопку 'Отменить' "
              + "\nдля возврата в предыдущий интерфейс программы !!!";
        return message;
    }
    
    public void addRecordDB() {
        // Извлекаем значения полей интерфейсной формы добавления новой записи 
        // и преобразуем их к типу полей таблицы Income_Expenses нашей БД
        Date dateOperation      = null;
        try {
        dateOperation       = new SimpleDateFormat(
                                    "dd-MM-yyyy", new Locale("ru")).
                                    parse(dateField.getText().trim());
        } catch (ParseException ex) {
            Logger.getLogger(MainClass.class.getName()).log(Level.SEVERE, null, ex);
        }
        float sumOperation      = Float.parseFloat(sumField.getText().trim());
        long operationTypeId = 0;
        switch (typeOperation) {
            case("CardIncome"): operationTypeId = 1; break;
            case("CardExpense"): operationTypeId = 2; break;
            case("CardAsset"): operationTypeId = 3; break;
        }
        String operationName    = kindField.getText().trim();
        long orgOrPersonTypeId   = ((String) 
                                    orgOrPersonTypeField.getSelectedItem()).
                                    equals("Лицо") ? 1 : 2;
        String orgOrPersonName  = orgOrPersonField.getText().trim();
        String description      = descriptionField.getText().trim();
        
        
        // Пересоздаем (обновляем) результирующий набор "crs" каждый раз 
        // перед вставкой новой строки в БД.
        fillTableData(null);
        
        // Сохраняем преобразованные данные в строке вставки результирующего 
        // набора "crs" и вставляем новую строку в таблицу Income_Expenses БД
        try {
            crs.moveToInsertRow();
            crs.updateDate("Date_operation", new java.sql.Date(dateOperation.getTime()));
            crs.updateFloat("Sum_operation", sumOperation);
            crs.updateLong("Id_operation_type", operationTypeId);
            crs.updateString("Operation_name", operationName);
            crs.updateLong("Id_org_person_type", orgOrPersonTypeId);
            crs.updateString("Org_person_name", orgOrPersonName);
            crs.updateString("Description", description);
            crs.insertRow();
            crs.moveToCurrentRow();
        } catch (SQLException ex) {
            Logger.getLogger(MainClass.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void changeRecordDB() {
        // Извлекаем значения полей интерфейсной формы редактирования записи 
        // и преобразуем их к типу полей таблицы Income_Expenses нашей БД
        Date dateOperation      = null;
        try {
        dateOperation       = new SimpleDateFormat(
                                    "dd-MM-yyyy", new Locale("ru")).
                                    parse(dateField.getText().trim());
        } catch (ParseException ex) {
            Logger.getLogger(MainClass.class.getName()).log(Level.SEVERE, null, ex);
        }
        float sumOperation      = Float.parseFloat(sumField.getText().trim());
        long operationTypeId = 0;
        switch (typeOperation) {
            case("ListIncomes"): operationTypeId = 1; break;
            case("ListExpenses"): operationTypeId = 2; break;
            case("ListAssets"): operationTypeId = 3; break;
        }
        String operationName    = kindField.getText().trim();
        long orgOrPersonTypeId   = ((String) 
                                    orgOrPersonTypeField.getSelectedItem()).
                                    equals("Лицо") ? 1 : 2;
        String orgOrPersonName  = orgOrPersonField.getText().trim();
        String description      = descriptionField.getText().trim();
        
        // Обновляем преобразованные данные в текущей строке результирующего 
        // набора "crs" и в таблице Income_Expenses БД
        try {
            crs.updateDate("Date_operation", new java.sql.Date(dateOperation.getTime()));
            crs.updateFloat("Sum_operation", sumOperation);
            crs.updateLong("Id_operation_type", operationTypeId);
            crs.updateString("Operation_name", operationName);
            crs.updateLong("Id_org_person_type", orgOrPersonTypeId);
            crs.updateString("Org_person_name", orgOrPersonName);
            crs.updateString("Description", description);
            crs.updateDate("Change_date", new 
                            java.sql.Date(new java.util.Date().getTime()));
            crs.updateRow();
        } catch (SQLException ex) {
            Logger.getLogger(MainClass.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void buildDetailsGui(JPanel parentPanel, JMenuBar menuBar, 
                                int width, int height, 
                                boolean isPack, boolean isCentered, 
                                int mode, long rowIdValue) {
        if (rowIdValue == 0) {
            JOptionPane.showMessageDialog(frame, "   Чтобы просмотреть "
                    + "детали, нужной перейти в список \n"
                    + "записей расходов (активов) и выбрать строку "
                    + "в списке !!!",
                    "Предупреждение", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        JPanel canvasPanel = new JPanel(new BorderLayout());
        canvasPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        String title = guiTitles.getProperty(typeOperation);
        
        JPanel backgroundDetails = new JPanel(new BorderLayout());
        Border border = BorderFactory.createLineBorder(Color.GRAY);
        backgroundDetails.setBorder(BorderFactory.createTitledBorder(border, title));
        
        MyDetailsModel detailsModel = new MyDetailsModel(rowIdValue);
        JTable detailsTable = new JTable(detailsModel);
        detailsTable.setFillsViewportHeight(true);
        // detailsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        detailsTable.setAutoCreateRowSorter(true);
        detailsTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        detailsTable.getColumnModel().getColumn(1).setPreferredWidth(60);
        detailsTable.getColumnModel().getColumn(2).setPreferredWidth(60);
        detailsTable.getColumnModel().getColumn(3).setPreferredWidth(60);
        detailsTable.getColumnModel().getColumn(4).setPreferredWidth(60);
        JScrollPane scroller = new JScrollPane(detailsTable);
        
        JButton addButton = new JButton("Добавить");
        JButton editButton = new JButton("Редактировать");
        JButton deleteButton = new JButton("Удалить");
        JButton returnButton = new JButton("Вернуться");
        addButton.addActionListener(new MyAddDetailListener(detailsModel, rowIdValue));
        editButton.addActionListener(new MyEditDetailListener(detailsTable, detailsModel));
        deleteButton.addActionListener(new MyDeleteDetailListener(detailsTable, detailsModel));
        returnButton.addActionListener(event -> drawApplicationGui(parentPanel, 
                                menuBar, width, height, isPack, isCentered));
        
        int columnsForButtons = 4;                      // <- для режима редактирования
        if (mode == VIEW_MODE) columnsForButtons = 3;   // <- для режима просмотра
        JPanel buttonsPanel = new JPanel(new GridLayout(1, columnsForButtons));
        buttonsPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        
        switch (mode) {
            case(EDIT_MODE):
                buttonsPanel.add(addButton);
                buttonsPanel.add(editButton);
                buttonsPanel.add(deleteButton);
                buttonsPanel.add(returnButton);
                break;
            case(VIEW_MODE):
                buttonsPanel.add(new JLabel());
                buttonsPanel.add(returnButton);
                buttonsPanel.add(new JLabel());
                break;
        }
        
        backgroundDetails.add(BorderLayout.CENTER, scroller);
        backgroundDetails.add(BorderLayout.SOUTH, buttonsPanel);
        
        canvasPanel.add(backgroundDetails);
        
        frame.setContentPane(canvasPanel);
        frame.setVisible(true);
    }
    
    class MyAddDetailListener implements ActionListener {
        MyDetailsModel  detailsModel;
        long            rowIdValue;
        
        public MyAddDetailListener(MyDetailsModel detailsModel, 
                                    long rowIdValue) {
            this.detailsModel = detailsModel;
            this.rowIdValue = rowIdValue;
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            JPanel inputDetailPanel = new JPanel(new GridLayout(5,2));
            
            JTextField nameField = new JTextField("", 10);
            MaskFormatter formatterQ = null;
            MaskFormatter formatterS = null;
            try {
                formatterQ     = new MaskFormatter("###");
                formatterS     = new MaskFormatter("###");
                formatterQ.setPlaceholderCharacter('0');
                formatterS.setPlaceholderCharacter('0');
            } catch (ParseException ex) {
                Logger.getLogger(MainClass.class.getName()).log(Level.SEVERE, null, ex);
            }
            JFormattedTextField quantityField = new JFormattedTextField(formatterQ);
            JComboBox<String>   quantityTypeField = new JComboBox<>(quantityArr);
                                quantityTypeField.setSelectedIndex(0);  // <- Выбран первый элемент в списке
            JFormattedTextField sizesField = new JFormattedTextField(formatterS);
            JComboBox<String>   sizesTypeField = new JComboBox<>(sizesArr);
                                sizesTypeField.setSelectedIndex(0);     // <- Выбран первый элемент в списке
                                
            inputDetailPanel.add(new JLabel("Наименование:"));
            inputDetailPanel.add(nameField);
            inputDetailPanel.add(new JLabel("Количество:"));
            inputDetailPanel.add(quantityField);
            inputDetailPanel.add(new JLabel("Ед.измерения:"));
            inputDetailPanel.add(quantityTypeField);
            inputDetailPanel.add(new JLabel("Объём (размер):"));
            inputDetailPanel.add(sizesField);
            inputDetailPanel.add(new JLabel("Ед.измерения:"));
            inputDetailPanel.add(sizesTypeField);
            
            int result = JOptionPane.showConfirmDialog(frame, 
                    inputDetailPanel, "Укажите детали", 
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (result == JOptionPane.OK_OPTION) {
                String n        = nameField.getText().trim();
                int    q        = Integer.parseInt(quantityField.getText().trim());
                String qType    = ((String)quantityTypeField.getSelectedItem()).trim();
                int    s        = Integer.parseInt(sizesField.getText().trim());
                String sType    = ((String)sizesTypeField.getSelectedItem()).trim();
                if (n.equals("") || q == 0 || 
                        quantityTypeField.getSelectedIndex() == -1)
                    JOptionPane.showMessageDialog(frame, "Не заполнено одно из "
                            + "первых трёх полей : "
                            + "\n-'Наименование', "
                            + "\n-'Количество', "
                            + "\n-'Ед.измерения' "
                            + "\nОбязательно заполните эти поля, чтобы "
                            + "\nдобавить детали о затратах или активах!",
                        "Предупреждение", JOptionPane.WARNING_MESSAGE);
                else {
                    try {
                        crsDetails.moveToInsertRow();
                        crsDetails.updateLong("Id_ie", rowIdValue);
                        crsDetails.updateObject("Name", n);
                        crsDetails.updateInt("Quantity", q);
                        crsDetails.updateString("Quantity_type", qType);
                        crsDetails.updateInt("Sizes", s);
                        crsDetails.updateString("Sizes_type", s == 0 ? sizesArr[0] : sType);
                        crsDetails.insertRow();
                        crsDetails.moveToCurrentRow();
                    } catch (SQLException ex) {
                        Logger.getLogger(MainClass.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    detailsModel.fireTableDataChanged();
                }
            }
        }
    }
    
    class MyEditDetailListener implements ActionListener {
        JTable          detailsTable;
        MyDetailsModel  detailsModel;
        
        public MyEditDetailListener(JTable detailsTable, 
                                    MyDetailsModel detailsModel) {
            this.detailsTable = detailsTable;
            this.detailsModel = detailsModel;
        }
        
        @Override
        public void actionPerformed(ActionEvent a) {
            int rowIndex = detailsTable.getSelectedRow();
            rowIndex = detailsTable.convertRowIndexToModel(rowIndex);
            if (rowIndex > -1) {
                // Создаем панель редактирования деталей
                JPanel inputDetailPanel = new JPanel(new GridLayout(5,2));
                
                // Создаем поля ввода для панели редактирования деталей
                JTextField nameField = new JTextField("", 10);
                MaskFormatter formatterQ = null;
                MaskFormatter formatterS = null;
                try {
                    formatterQ     = new MaskFormatter("###");
                    formatterS     = new MaskFormatter("###");
                    formatterQ.setPlaceholderCharacter('0');
                    formatterS.setPlaceholderCharacter('0');
                } catch (ParseException ex) {
                    Logger.getLogger(MainClass.class.getName()).log(Level.SEVERE, null, ex);
                }
                JFormattedTextField quantityField = new JFormattedTextField(formatterQ);
                JComboBox<String>   quantityTypeField = new JComboBox<>(quantityArr);
                JFormattedTextField sizesField = new JFormattedTextField(formatterS);
                JComboBox<String>   sizesTypeField = new JComboBox<>(sizesArr);
                
                // Инициализируем поля ввода значениями полей из текущей строки 
                // табличной модели (по сути - значениями полей текущей строки 
                // из ResultSet "crsDetails")
                nameField.          setText(detailsModel.
                                    getValueAt(rowIndex, 0).toString());
                
                String value = ((Number) detailsModel.getValueAt(rowIndex, 1)).toString();
                if (value.length() < 3)
                    value = "0".repeat(3 - value.length()) + value;
                quantityField.      setText(value);
                quantityTypeField.  setSelectedItem((String) 
                                    detailsModel.getValueAt(rowIndex, 2));
                
                value = ((Number) detailsModel.getValueAt(rowIndex, 3)).toString();
                if (value.length() < 3)
                    value = "0".repeat(3 - value.length()) + value;
                sizesField.         setText(value);
                sizesTypeField.     setSelectedItem((String) 
                                    detailsModel.getValueAt(rowIndex, 4));
                nameField.requestFocus();
                
                // Запоминаем исходные значения полей ввода до их редактиро- 
                // вания в промежуточных контрольных (эталонных) переменных
                String checkN        = nameField.getText().trim();
                int    checkQ        = Integer.parseInt(quantityField.getText().trim());
                String checkQType    = ((String)quantityTypeField.getSelectedItem()).trim();
                int    checkS        = Integer.parseInt(sizesField.getText().trim());
                String checkSType    = ((String)sizesTypeField.getSelectedItem()).trim();
                
                // Размещаем метки и поля ввода на панели редактитирования
                inputDetailPanel.add(new JLabel("Наименование:"));
                inputDetailPanel.add(nameField);
                inputDetailPanel.add(new JLabel("Количество:"));
                inputDetailPanel.add(quantityField);
                inputDetailPanel.add(new JLabel("Ед.измерения:"));
                inputDetailPanel.add(quantityTypeField);
                inputDetailPanel.add(new JLabel("Объём (размер):"));
                inputDetailPanel.add(sizesField);
                inputDetailPanel.add(new JLabel("Ед.измерения:"));
                inputDetailPanel.add(sizesTypeField);
                
                // - Отображаем панель редактирования в диалоговом окне. 
                // - Проверяем выбор пользователя в этом окне.  
                // - Выпоняем обработку значений в полях ввода диалогового окна. 
                // - Сохраняем измененные значения в ResultSet и в БД (при 
                //   выполнении проверок).
                int result = JOptionPane.showConfirmDialog(frame, 
                    inputDetailPanel, "Укажите детали", 
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (result == JOptionPane.OK_OPTION) {
                    String n        = nameField.getText().trim();
                    int    q        = Integer.parseInt(quantityField.getText().trim());
                    String qType    = ((String)quantityTypeField.getSelectedItem()).trim();
                    int    s        = Integer.parseInt(sizesField.getText().trim());
                    String sType    = ((String)sizesTypeField.getSelectedItem()).trim();
                    if (n.equals("") || q == 0 || 
                            quantityTypeField.getSelectedIndex() == -1)
                        JOptionPane.showMessageDialog(frame, "Не заполнено одно из "
                                + "первых трёх полей : "
                                + "\n-'Наименование', "
                                + "\n-'Количество', "
                                + "\n-'Ед.измерения' "
                                + "\nОбязательно заполните эти поля, чтобы "
                                + "\nдобавить детали о затратах или активах!",
                            "Предупреждение", JOptionPane.WARNING_MESSAGE);
                    else {
                        try {
                            if (!checkN.equals(n))
                                crsDetails.updateString("Name", n);
                            if (checkQ != q)
                                crsDetails.updateInt("Quantity", q);
                            if (!checkQType.equals(qType))
                                crsDetails.updateString("Quantity_type", qType);
                            if (checkS != s)
                                crsDetails.updateInt("Sizes", s);
                            if (!checkSType.equals(sType))
                                crsDetails.updateString("Sizes_type", s == 0 ? sizesArr[0] : sType);
                            crsDetails.updateDate("Change_date", new 
                                java.sql.Date(new java.util.Date().getTime()));
                            crsDetails.updateRow();
                        } catch (SQLException ex) {
                            Logger.getLogger(MainClass.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        detailsModel.fireTableDataChanged();
                    }
                } 
            } else
                JOptionPane.showMessageDialog(frame, "Чтобы отредактировать "
                        + "запись, выберите строку в списке !!!",
                        "Предупреждение", JOptionPane.WARNING_MESSAGE);
        }
    }
    
    class MyDeleteDetailListener implements ActionListener {
        JTable          detailsTable;
        MyDetailsModel  detailsModel;
        
        public MyDeleteDetailListener(JTable detailsTable, 
                                    MyDetailsModel detailsModel) {
            this.detailsTable = detailsTable;
            this.detailsModel = detailsModel;
        }
        
        @Override
        public void actionPerformed(ActionEvent a) {
            int[] selection = detailsTable.getSelectedRows();
            for (int i = 0; i < selection.length; i++)
                selection[i] = detailsTable.convertRowIndexToModel(selection[i]);
            if (selection.length > 0) {
                int confirm = JOptionPane.showConfirmDialog(frame, 
                        "Вы действительно хотите удалить запись из списка?" +
                        "\n    Данная операция является необратимой !!!", 
                        "Подтверждение", 
                        JOptionPane.YES_NO_OPTION, 
                        JOptionPane.QUESTION_MESSAGE);
                if (confirm == JOptionPane.YES_OPTION) {
                    try {
                        for (int i = selection.length - 1; i >= 0; i--)
                            if (crsDetails.absolute(selection[i] + 1))
                                crsDetails.deleteRow();
                    } catch (SQLException ex) {
                            Logger.getLogger(MainClass.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    detailsModel.fireTableDataChanged();
                }
            } else {
                JOptionPane.showMessageDialog(frame, "Выберите запись в списке"
                        + " чтобы выполнить ее удаление !!!",
                        "Предупреждение", JOptionPane.WARNING_MESSAGE);
            }
        }
    }
    
    class MyDetailsModel extends AbstractTableModel {
        String[] columnNames = {"Наименование", "Количество", "Ед.изм.", 
                                "Объем/размер", "Ед.изм."};
        
        public MyDetailsModel(long rowIdValue) {
            String query = "SELECT Simple_Details.Name, "
                            + "Simple_Details.Quantity, "
                            + "Simple_Details.Quantity_type, "
                            + "Simple_Details.Sizes, "
                            + "Simple_Details.Sizes_type, "
                            + "Simple_Details.Id_ie, "
                            + "Simple_Details.Change_date, "
                            + "Simple_Details.Id "
                    + "FROM Simple_Details, Income_Expenses "
                    + "WHERE Simple_Details.Id_ie = Income_Expenses.Id "
                            + " AND Income_Expenses.Id  = ?";
        
            try {
                if (crsDetails != null)
                    crsDetails.close();
                PreparedStatement prepStat = conn.prepareStatement(query, 
                                        ResultSet.TYPE_SCROLL_INSENSITIVE, 
                                        ResultSet.CONCUR_UPDATABLE);
                prepStat.setLong(1, rowIdValue);
                crsDetails = prepStat.executeQuery();
            } catch (SQLException ex) {
                Logger.getLogger(MainClass.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        @Override
        public int getRowCount() {
            int rowCount = 0;
            try {
                crsDetails.last();
                rowCount = crsDetails.getRow();
                crsDetails.absolute(1);
            } catch (SQLException ex) {
                Logger.getLogger(MainClass.class.getName()).log(Level.SEVERE, null, ex);
            }
            return rowCount;
        }
        
        @Override
        public int getColumnCount() {
            return columnNames.length;
        }
        
        @Override
        public String getColumnName​(int column) {
            return columnNames[column];
        }
        
        @Override
        public Class getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }
        
        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Object value = null;
            try {
                crsDetails.absolute(rowIndex+1);
                value = crsDetails.getObject(columnIndex + 1);
            } catch (SQLException ex) {
                Logger.getLogger(MainClass.class.getName()).log(Level.SEVERE, null, ex);
            }
            return value;
        }
    }
    
    /*
     * Далее расположен код, который отвечает за отрисовку ГПИ и логику 
     * отображения и обработки списков записей расходов, доходов, активов.
     */
    
    void buildGuiOut() {
        backgroundOut = new JPanel(new BorderLayout());
        Border emptyBorder = BorderFactory.createEmptyBorder(10, 10, 10, 10);
        backgroundOut.setBorder(BorderFactory.createTitledBorder(
                            emptyBorder, guiTitles.getProperty(typeOperation)));
        
        myModel = new MyTableModel();
        table = new JTable(myModel);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        for (int i = 0; i < table.getColumnCount(); i++) {
            int widthColumn = 100;
            switch(table.getColumnName(i)) {
                case ("№ п/п"): widthColumn = 30; break;
                case ("Дата"):   widthColumn = 70; break;
                case ("Сумма"):  widthColumn = 60; break;
            }
            table.getColumnModel().getColumn(i).setPreferredWidth(widthColumn);
        }
        JScrollPane scroller = new JScrollPane(table);
        
        JButton openCardButton = new JButton("Просмотреть");
        JButton editCardButton = new JButton("Редактировать");
        JButton deleteCardButton = new JButton("Удалить");
        JButton comeBackButton = new JButton("Вернуться");
        openCardButton.addActionListener(event -> new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent a) {
                int rowIndex = table.getSelectedRow();
                rowIndex = table.convertRowIndexToModel(rowIndex);
                if (rowIndex > -1) {
                    long rowIdValue = (Long) myModel.getValueAt(rowIndex, 0);
                    buildGuiCard(rowIndex, VIEW_MODE, rowIdValue);
                } else
                    JOptionPane.showMessageDialog(frame, "Чтобы перейти "
                            + "к просмотру записи выберите строку в списке !!!",
                            "Предупреждение", JOptionPane.WARNING_MESSAGE);
            }
        }.actionPerformed(event));
        editCardButton.addActionListener(event -> new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent a) {
                int rowIndex = table.getSelectedRow();
                rowIndex = table.convertRowIndexToModel(rowIndex);
                if (rowIndex > -1) {
                    long rowIdValue = (Long) myModel.getValueAt(rowIndex, 0);
                    buildGuiCard(rowIndex, EDIT_MODE, rowIdValue);
                } else
                    JOptionPane.showMessageDialog(frame, "Чтобы отредактировать"
                            + " запись, выберите строку в списке !!!",
                            "Предупреждение", JOptionPane.WARNING_MESSAGE);
            }
        }.actionPerformed(event));
        deleteCardButton.addActionListener(new MyDeleteDataListener());
        comeBackButton.addActionListener(event -> drawApplicationGui(
                                    background, menuBar, 300, 300, true, true));
        
        JPanel panelButton = new JPanel(new GridLayout(1, 4, 10, 0));
        panelButton.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));
        panelButton.add(openCardButton);
        panelButton.add(editCardButton);
        panelButton.add(deleteCardButton);
        panelButton.add(comeBackButton);
        
        backgroundOut.add(BorderLayout.CENTER, scroller);
        backgroundOut.add(BorderLayout.SOUTH, panelButton);
        
        drawApplicationGui(backgroundOut, null, 590, 300, false, true);
    }
    
    class MyDeleteDataListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent a) {
            // Здесь  на  самом  деле   можно   использовать  метод 
            // table.getSelectedRow(),  возвращающий индекс  только 
            // одной выделенной записи в таблице, так как в таблице 
            // установлен режим выбора одиночных строк
            
            int[] selection = table.getSelectedRows();
            for (int i = 0; i < selection.length; i++)
                selection[i] = table.convertRowIndexToModel(selection[i]);
            if (selection.length > 0) {
                int confirm = JOptionPane.showConfirmDialog(frame, 
                        "Вы действительно хотите удалить запись из списка?" +
                        "\n    Данная операция является необратимой !!!", 
                        "Подтверждение", 
                        JOptionPane.YES_NO_OPTION, 
                        JOptionPane.QUESTION_MESSAGE);
                if (confirm == JOptionPane.YES_OPTION) {
                    try {
                        for (int i = selection.length - 1; i >= 0; i--)
                            if (crs.absolute(selection[i] + 1))
                                crs.deleteRow();
                    } catch (SQLException ex) {
                            Logger.getLogger(MainClass.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    myModel.fireTableDataChanged();
                }
            } else {
                JOptionPane.showMessageDialog(frame, "Выберите запись в списке"
                        + " чтобы выполнить ее удаление !!!",
                        "Предупреждение", JOptionPane.WARNING_MESSAGE);
            }
        }
    }
    
    class MyTableModel extends AbstractTableModel {
        String[] columnNames;       // Массив текущей шапки таблицы, которому 
                                    // присваивается один из ниже следующих:
        String[] columnNamesIncomes  = {"№ п/п", "Дата", "Сумма", "Вид дохода", 
                                        "Источник дохода", "Описание"};
        String[] columnNamesExpenses = {"№ п/п", "Дата", "Сумма", "Вид расходов", 
                                        "Где затрачено", "Описание"};
        String[] columnNamesAssets   = {"№ п/п", "Дата", "Сумма", "Вид актива", 
                                        "Где приобретено", "Описание"};
    
        public MyTableModel() {
            switch (typeOperation) {
                case ("ListIncomes"):
                    columnNames = columnNamesIncomes;
                    fillTableData("Доходы");
                    break;
                case ("ListExpenses"):
                    columnNames = columnNamesExpenses;
                    fillTableData("Расходы");
                    break;
                case ("ListAssets"):
                    columnNames = columnNamesAssets;
                    fillTableData("Активы");
                    break;
            }
        }
        
        @Override
        public int getRowCount() {
            int rowCount = 0;
            try {
                crs.last();
                rowCount = crs.getRow();
                crs.absolute(1);
            } catch (SQLException ex) {
                Logger.getLogger(MainClass.class.getName()).log(Level.SEVERE, null, ex);
            }
            return rowCount;
        }
        
        @Override
        public int getColumnCount() {
            return columnNames.length;
        }
        
        @Override
        public String getColumnName​(int column) {
            return columnNames[column];
        }
        
        @Override
        public Class getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }
        
        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Object value = null;
            try {
                crs.absolute(rowIndex+1);
                value = crs.getObject(columnIndex + 1);
            } catch (SQLException ex) {
                Logger.getLogger(MainClass.class.getName()).log(Level.SEVERE, null, ex);
            }
            return value;
        }
    }
    
    final void fillTableData(String operationTypeName) {
        String query;
        
        if (operationTypeName != null)
            query = "SELECT Income_Expenses.Id, "
                            + "Income_Expenses.Date_operation, "
                            + "Income_Expenses.Sum_operation, "
                            + "Income_Expenses.Operation_name, "
                            + "Income_Expenses.Org_person_name, "
                            + "Income_Expenses.Description, "
                            + "Income_Expenses.Id_operation_type, "
                            + "Income_Expenses.Id_org_person_type, "
                            + "Income_Expenses.Change_date " 
                    + "FROM Income_Expenses, Operation_Types "
                    + "WHERE Income_Expenses.Id_operation_type = "
                            + "Operation_Types.Id AND "
                            + "Operation_Types.Operation_type_name = ?";
        else
            query = "SELECT Income_Expenses.Id, "
                            + "Income_Expenses.Date_operation, "
                            + "Income_Expenses.Sum_operation, "
                            + "Income_Expenses.Operation_name, "
                            + "Income_Expenses.Org_person_name, "
                            + "Income_Expenses.Description, "
                            + "Income_Expenses.Id_operation_type, "
                            + "Income_Expenses.Id_org_person_type, "
                            + "Income_Expenses.Change_date " 
                    + "FROM Income_Expenses";
        try {
            if (crs != null)
                crs.close();
            PreparedStatement prepStat = conn.prepareStatement(query, 
                                    ResultSet.TYPE_SCROLL_INSENSITIVE, 
                                    ResultSet.CONCUR_UPDATABLE);
            if (operationTypeName != null)
                prepStat.setString(1, operationTypeName);
            crs = prepStat.executeQuery();
        } catch (SQLException ex) {
            Logger.getLogger(MainClass.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /*
     * Далее расположен код, который отвечает за отрисовку ГПИ и логику 
     * отображения балансовых (итоговых) значений по расходам, доходам, 
     * сальдо между расходами и доходами, а также активам.
     */
    
    public void buildGuiBalance() {
        JPanel canvasPanel = new JPanel(new BorderLayout());
        canvasPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        JPanel backgroundBalance = new JPanel(new BorderLayout());
        Border border = BorderFactory.createLineBorder(Color.GRAY);
        backgroundBalance.setBorder(BorderFactory.createTitledBorder(border, 
                                        guiTitles.getProperty(typeOperation)));
        
        MyBalanceModel balanceModel = new MyBalanceModel();
        JTable balanceTable = new JTable(balanceModel);
        balanceTable.setFillsViewportHeight(true);
        JScrollPane scroller = new JScrollPane(balanceTable);
        
        JPanel msgPanel = new JPanel();
        msgPanel.setLayout(new BoxLayout(msgPanel, BoxLayout.PAGE_AXIS));
        msgPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        String[] message = {"ОТЛИЧНО !!!", 
                            "Ваши расходы НЕ ПРЕВЫШАЮТ ваши доходы !!!"};
        if ((Float)balanceModel.getValueAt(0, 2) < 0) { 
            message[0] = "ВНИМАНИЕ !!! ";
            message[1] = "Ваши расходы ПРЕВЫШАЮТ ваши доходы !!!";
        }
        JLabel lbl_1 = new JLabel(message[0]);
        JLabel lbl_2 = new JLabel(message[1]);
        lbl_1.setAlignmentX(Component.CENTER_ALIGNMENT);
        lbl_2.setAlignmentX(Component.CENTER_ALIGNMENT);
        msgPanel.add(lbl_1);
        msgPanel.add(lbl_2);
        
        JPanel buttonsPanel = new JPanel(new GridLayout(1,3));
        buttonsPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        
        JButton returnButton = new JButton("Вернуться");
        returnButton.addActionListener(event -> drawApplicationGui(
                                background, menuBar, 300, 300, true, true));
        
        buttonsPanel.add(new JLabel());
        buttonsPanel.add(returnButton);
        buttonsPanel.add(new JLabel());
        
        backgroundBalance.add(BorderLayout.NORTH, msgPanel);
        backgroundBalance.add(BorderLayout.CENTER, scroller);
        backgroundBalance.add(BorderLayout.SOUTH, buttonsPanel);
        
        canvasPanel.add(backgroundBalance);
        
        frame.setContentPane(canvasPanel);
        frame.setVisible(true);
    }
    
    class MyBalanceModel extends AbstractTableModel {
        ArrayList<Float> balTable = new ArrayList<>();
        String[] columnNames = {"Доходы", "Расходы", 
                                "Сальдо", "Активы"};
        
        public MyBalanceModel() {
            balTable = new ArrayList<>();
            String query; 
            query = "SELECT (SELECT SUM(sum_operation) " + 
                                "FROM income_expenses " + 
                                "WHERE Id_operation_type = 1) AS Incomes, " + 
                           "(SELECT SUM(sum_operation) " + 
                                "FROM income_expenses " + 
                                "WHERE Id_operation_type = 2) AS Expenses, " + 
                           "(SELECT (SELECT SUM(sum_operation) " + 
                                        "FROM income_expenses " + 
                                        "WHERE Id_operation_type = 1) - " + 
                                   "(SELECT SUM(sum_operation) " + 
                                        "FROM income_expenses " + 
                                        "WHERE Id_operation_type = 2)) AS Saldo, " + 
                           "(SELECT sum(sum_operation) " + 
                                "FROM income_expenses " + 
                                "WHERE Id_operation_type = 3) AS Assets;";
            try (Statement stat = conn.createStatement()) {
                ResultSet result = stat.executeQuery(query);
                if (result.next()) {
                    balTable.add(result.getFloat("Incomes"));
                    balTable.add(result.getFloat("Expenses"));
                    balTable.add(result.getFloat("Saldo"));
                    balTable.add(result.getFloat("Assets"));
                }
            } catch (SQLException ex) {
                    Logger.getLogger(MainClass.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        @Override
        public int getRowCount() {
            return 1;
        }
        
        @Override
        public int getColumnCount() {
            return 4;
        }
        
        @Override
        public String getColumnName​(int column) {
            return columnNames[column];
        }
        
        @Override
        public Class getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }
        
        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Float value = 0f;
            switch (columnIndex) {
                case (0):
                    value = balTable.get(0);
                    break;
                case (1):
                    value = balTable.get(1);
                    break;
                case (2):
                    value = balTable.get(2);
                    break;
                case (3):
                    value = balTable.get(3);
                    break;
            }
            return value;
        }
    }
}
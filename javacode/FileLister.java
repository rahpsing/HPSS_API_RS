/**
author : Rahul Singh
The following code creates a sample WinSCP like GUI and lists directory contents
of local system and HPSS system which the user is knitted to.
File transfers are achieved using the HPSS API by invoking C executables.
 **/

import java.awt.Button;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.List;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JSplitPane;
import javax.swing.ListCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Date;
import java.util.StringTokenizer;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

/**
 * This class creates and displays a window containing a list of files and
 * sub-directories in a specified directory. Clicking on an entry in the list
 * displays more information about it. Double-clicking on an entry displays it,
 * if a file, or lists it if a directory. An optionally-specified FilenameFilter
 * filters the displayed list.
 */
public class FileLister extends Panel implements ActionListener, ItemListener {
  private List list; // To display the directory contents in
  private JList HPSSList; // To display the directory contents in
  private static JTable HPSSFileTable,localFileTable;
  private TextField details; // To display detail info in.
  private TextField HPSSFileDetails; // To display detail info in.
  private JScrollPane localScrollPane,HPSSScrollPane;
  private Panel buttons, HPSSButtons; // Holds the buttons

  private JButton up, close, sendToHPSS, getFromHPSS; // The Up and Close buttons

  private static File currentDir; // The directory currently listed

  private FilenameFilter filter; // An optional filter for the directory
  private static String basePath;
  private static String currentHPSSDirectory;
  private static String localDirectory;
  private String[] files; // The directory contents
    private String[][] HPSSFiles,localFiles; // The directory contents
  private static String columns[] = {"Name","Size(Bytes)","FileType","Last Modified","AbsolutePath"};
  private DateFormat dateFormatter = // To display dates and time correctly
  DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

  /**
   * Constructor: create the GUI, and list the initial directory.
   */
  public FileLister(String directory, FilenameFilter filter, boolean isHPSS) {

    this.filter = filter; // Save the filter, if any

    HPSSList = new JList();
    HPSSList.setFont(new Font("MonoSpaced", Font.PLAIN, 14));

    list = new List(12, false); // Set up the list
    list.setFont(new Font("MonoSpaced", Font.PLAIN, 14));
    list.addActionListener(this);
    list.addItemListener(this);
  
    TabListCellRenderer renderer = new TabListCellRenderer();
    HPSSList.setCellRenderer(renderer);

    details = new TextField(); // Set up the details area
    details.setFont(new Font("MonoSpaced", Font.PLAIN, 12));
    details.setEditable(false);

    HPSSFileDetails = new TextField(); // Set up the details area
    HPSSFileDetails.setFont(new Font("MonoSpaced", Font.PLAIN, 12));
    HPSSFileDetails.setEditable(false);

    buttons = new Panel(); // Set up the button box
    buttons.setLayout(new FlowLayout(FlowLayout.RIGHT, 15, 5));
    buttons.setFont(new Font("SansSerif", Font.BOLD, 14));

    HPSSButtons = new Panel();
    HPSSButtons.setLayout(new FlowLayout(FlowLayout.RIGHT, 15, 5));
    HPSSButtons.setFont(new Font("SansSerif", Font.BOLD, 14));

    up = new JButton("Up a Directory"); // Set up the two buttons
    close = new JButton("Close");
    sendToHPSS = new JButton(">>>>>");
    sendToHPSS.addActionListener(this);
    if(isHPSS) {
        getFromHPSS = new JButton("<<<<<"); // Set up the two buttons 
        getFromHPSS.addActionListener(this);
        HPSSButtons.add(getFromHPSS);
    }
  
    up.addActionListener(this);
    close.addActionListener(this);

    buttons.add(up); // Add buttons to button box
    buttons.add(close);
  

    this.setSize(800, 800);

    if(isHPSS) {

        HPSSFileTable = new JTable();
        listHPSSDirectory("/hpss/r/a/rahpsing/"); // And now list initial directory.
        HPSSScrollPane = new JScrollPane(HPSSFileTable);
        this.add(HPSSScrollPane, "Center"); // Add stuff to the window
        this.add(HPSSFileDetails, "North");
        this.add(HPSSButtons, "South");
        HPSSFileTable.setShowGrid(false);
    }else {
        
    localFileTable = new JTable();
        buttons.add(sendToHPSS);
	listDirectory(directory);
        localScrollPane = new JScrollPane(localFileTable);              
        this.add(localScrollPane, "Center"); // Add stuff to the window
    	this.add(details, "North");
    	this.add(buttons, "South");
       
        localFileTable.setShowGrid(false);
    }
  }
 
 
  /**
   * This method uses the list() method to get all entries in a directory and
   * then displays them in the List component.
   */
  public void listHPSSDirectory(String directoryName) {

    HPSSFiles = getListOfFiles(directoryName);
 
    DefaultTableModel objModel = new DefaultTableModel(HPSSFiles,columns);

    HPSSFileTable.setModel(objModel); 
    
    // Sort the list of filenames.
    HPSSFileTable.removeColumn(HPSSFileTable.getColumnModel().getColumn(4));   

    HPSSFileDetails.setText(directoryName);

    // Remember this directory for later.
    currentHPSSDirectory = directoryName;
  }

 
  private String executeCommand(String command) {

        StringBuffer output = new StringBuffer();

        Process p;
        try {
            p = Runtime.getRuntime().exec(command);
            p.waitFor();
            BufferedReader reader =
                          new BufferedReader(new InputStreamReader(p.getInputStream()));

                      String line = "";          
            while ((line = reader.readLine())!= null) {
                output.append(line + "\n");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return output.toString();

    }
 
 

  private String[][] getListOfFiles(String directoryName) {
    // TODO Auto-generated method stub
    
      return executeCommandLS(directoryName);
}

private boolean sendFile(String sourceFilePath, String HPSSDestination) {
        // TODO Auto-generated method stub
        JOptionPane.showMessageDialog(null,sourceFilePath);
        JOptionPane.showMessageDialog(null,HPSSDestination);
        String output = executeCommand("/tmp/HPSSAPI/hput/hput " + sourceFilePath + " " + HPSSDestination);
        JOptionPane.showMessageDialog(null,output);
        DefaultTableModel objModel = (DefaultTableModel)localFileTable.getModel();
        objModel.setRowCount(0);
        listHPSSDirectory(sourceFilePath);
        return true;
}

private boolean getFile(String sourceFilePath, String HPSSDestination) {
    // TODO Auto-generated method stub
    String output = executeCommand("/tmp/HPSSAPI/hget/hget " + HPSSDestination + " " + sourceFilePath);

    return true;
}

private String[][] executeCommandLS(String pathName) {
    // TODO Auto-generated method stub
    String output = executeCommand("/tmp/HPSSAPI/hlsa/hlsa " + pathName);
   // JOptionPane.showMessageDialog(null,output);
   String line[] = output.split("\\r?\\n");
   JOptionPane.showMessageDialog(null,line);   
   String returnData[][] = new String[line.length][5];
   int i = 0;
   for(String objString : line) {
       DirectoryInfo objDirInfo = new DirectoryInfo();
       String dirData[] = objString.split(",");
       returnData[i][0] = dirData[0]; 
       returnData[i][1] = dirData[1];
       returnData[i][2] = dirData[2];
       returnData[i][3] = DirectoryInfo.convertEpochToDate(dirData[3]);
       returnData[i][4] = pathName+ "" + dirData[0];
       i++;
   }
    return returnData;
}


/**
   * This method uses the list() method to get all entries in a directory and
   * then displays them in the List component.
   */
  public String[][] listDirectory(String directory) {
    // Convert the string to a File object, and check that the dir exists
    File dir = new File(directory);
  
    if (!dir.isDirectory())
      throw new IllegalArgumentException("FileLister: no such directory");

    // Get the (filtered) directory entries
    File[] listFiles = dir.listFiles();
    String returnData[][] = new String[listFiles.length][5];
    // Sort the list of filenames.
    java.util.Arrays.sort(listFiles);
    int i = 0;
    for(File objFile : listFiles) {
	returnData[i][0] = objFile.getName(); 
	returnData[i][1] = String.valueOf(objFile.length());
	returnData[i][2] = objFile.isFile()?"File":"Directory";
	returnData[i][3] = DirectoryInfo.convertEpochToDate(String.valueOf(objFile.lastModified()));
	returnData[i][4] = directory + "/" + objFile.getName();
        i++;
    }

    details.setText(directory);

    // Remember this directory for later.
    currentDir = dir;
    localDirectory = directory;
    DefaultTableModel objModel = new DefaultTableModel(returnData,columns);

    localFileTable.setModel(objModel); 
    // Sort the list of filenames.
    localFileTable.removeColumn(localFileTable.getColumnModel().getColumn(4));  
    return returnData;
  }

    public void reloadDirectory(String directory) {
	list.removeAll();    
    list = new List(12, false); // Set up the list
    list.setFont(new Font("MonoSpaced", Font.PLAIN, 14));
    list.addActionListener(this);
    list.addItemListener(this);

    }

  /**
   * This ItemListener method uses various File methods to obtain information
   * about a file or directory. Then it displays that info.
   */
  public void itemStateChanged(ItemEvent e) {
    int i = list.getSelectedIndex() - 1; // minus 1 for Up To Parent entry
    if (i < 0)
      return;
    String filename = files[i]; // Get the selected entry
    File f = new File(currentDir, filename); // Convert to a File
    if (!f.exists()) // Confirm that it exists
      throw new IllegalArgumentException("FileLister: " + "no such file or directory");

    // Get the details about the file or directory, concatenate to a string
    String info = filename;
    if (f.isDirectory())
      info += File.separator;
    info += " " + f.length() + " bytes ";
    info += dateFormatter.format(new java.util.Date(f.lastModified()));
    if (f.canRead())
      info += " Read";
    if (f.canWrite())
      info += " Write";

    // And display the details string
    details.setText(info);
  }

  /**
   * This ActionListener method is invoked when the user double-clicks on an
   * entry or clicks on one of the buttons. If they double-click on a file,
   * create a FileViewer to display that file. If they double-click on a
   * directory, call the listDirectory() method to display that directory
   */
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == close)
     // this.dispose();
        System.out.println("hello");
    else if (e.getSource() == up) {
      up();
    } else if (e.getSource() == list) { // Double click on an item
      int i = list.getSelectedIndex(); // Check which item
      if (i == 0)
        up(); // Handle first Up To Parent item
      else { // Otherwise, get filename
        String name = files[i - 1];
        File f = new File(currentDir, name); // Convert to a File
        String fullname = f.getAbsolutePath();
        if (f.isDirectory())
          listDirectory(fullname); // List dir
        else
          new FileViewer(fullname).show(); // display file
      }
    }  else if (e.getSource() == HPSSList) { // Double click on an item
        int i = HPSSList.getSelectedIndex(); // Check which item
        if (i == 0)
          up(); // Handle first Up To Parent item
        else { // Otherwise, get filename
          String name = files[i - 1];
          File f = new File(currentDir, name); // Convert to a File
          String fullname = f.getAbsolutePath();
          if (f.isDirectory())
            listDirectory(fullname); // List dir
          else
            new FileViewer(fullname).show(); // display file
        }
      } else if (e.getSource() == sendToHPSS) {
         int row = localFileTable.getSelectedRow();
          JOptionPane.showMessageDialog(null,row);
        if (row != 0) {
            String filePath = (String)localFileTable.getModel().getValueAt(row,4);
    
            long startTime = System.currentTimeMillis();
            boolean status = sendFile(filePath,currentHPSSDirectory);
            long endTime = System.currentTimeMillis();
            String message = status ? "Transfer is successful. \nTime taken = "+((endTime-startTime)/1000)+ " seconds" : "Unable to get file";
            JOptionPane.showMessageDialog(null,message);
            
        }
    } else if (e.getSource() == getFromHPSS) {
        int row = HPSSFileTable.getSelectedRow();
        
        if (row != 0) {
            String filePath = (String)HPSSFileTable.getModel().getValueAt(row,4);
 
            long startTime = System.currentTimeMillis();
            boolean status = getFile(currentDir.getAbsolutePath(), filePath);
            long endTime = System.currentTimeMillis();
            String message = status ? "Transfer is successful. \nTime taken = "+((endTime-startTime)/1000)+ " seconds" : "Unable to get file";
            JOptionPane.showMessageDialog(null,message);
                   DefaultTableModel objModel = (DefaultTableModel)localFileTable.getModel();
     objModel.setRowCount(0);
         localFileTable.revalidate();
     objModel.fireTableDataChanged();     	
     listDirectory(currentDir.getAbsolutePath());
            
        }
    }
  }

 

  /** A convenience method to display the contents of the parent directory */
  protected void up() {
    String parent = currentDir.getParent();
    if (parent == null)
      return;
    listDirectory(parent);
  }

  /** A convenience method used by main() */
  public static void usage() {
    System.out.println("Usage: java FileLister [directory_name] " + "[-e file_extension]");
    System.exit(0);
  }

  /**
   * A main() method so FileLister can be run standalone. Parse command line
   * arguments and create the FileLister object. If an extension is specified,
   * create a FilenameFilter for it. If no directory is specified, use the
   * current directory.
   */
  public static void main(String args[]) throws IOException {
    FileLister f,f2;
    FilenameFilter filter = null; // The filter, if any
    String directory = null; // The specified dir, or the current dir

    // Loop through args array, parsing arguments
    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("-e")) {
        if (++i >= args.length)
          usage();
        final String suffix = args[i]; // final for anon. class below

        // This class is a simple FilenameFilter. It defines the
        // accept() method required to determine whether a specified
        // file should be listed. A file will be listed if its name
        // ends with the specified extension, or if it is a directory.
        filter = new FilenameFilter() {
          public boolean accept(File dir, String name) {
            if (name.endsWith(suffix))
              return true;
            else
              return (new File(dir, name)).isDirectory();
          }
        };
      } else {
        if (directory != null)
          usage(); // If already specified, fail.
        else
          directory = args[i];
      }
    }

    // if no directory specified, use the current directory
    if (directory == null)
      directory = System.getProperty("user.dir");
    // Create the FileLister object, with directory and filter specified.
    f = new FileLister(directory, filter, false);
    f2 = new FileLister(directory, filter, true);
    // Arrange for the application to exit when the window is closed
    JSplitPane splitPane =  new JSplitPane();
    JFrame objFrame = new JFrame("File loader");
    objFrame.add(f);
    // Destroy the window when the user requests it
  
    f.setSize(new Dimension(300, 300));
    f2.setSize(new Dimension(300, 300));
    f.setLayout(new BoxLayout(f, BoxLayout.PAGE_AXIS));
    f2.setLayout(new BoxLayout(f2, BoxLayout.PAGE_AXIS));
  
    objFrame.setPreferredSize(new Dimension(800, 400));
    objFrame.getContentPane().setLayout(new GridLayout());
    objFrame.getContentPane().add(splitPane); 
  
  
    // let's configure our splitPane:
    splitPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);  // we want it to split the window verticaly
    splitPane.setDividerLocation(400);                    // the initial position of the divider is 200 (our window is 400 pixels high)
    splitPane.setLeftComponent(f);                // at the top we want our "topPanel"
    splitPane.setRightComponent(f2);
  
  
  
    objFrame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
          objFrame.dispose();
      }
    });
    objFrame.addWindowListener(new WindowAdapter() {
      public void windowClosed(WindowEvent e) {
        System.exit(0);
      }
    });
    // Finally, pop the window up up.
    objFrame.pack();
    objFrame.setVisible(true);
  }
}

class FileViewer extends Frame implements ActionListener {
  String directory; // The default directory to display in the FileDialog

  TextArea textarea; // The area to display the file contents into

  /** Convenience constructor: file viewer starts out blank */
  public FileViewer() {
    this(null, null);
  }

  /** Convenience constructor: display file from current directory */
  public FileViewer(String filename) {
    this(null, filename);
  }

  /**
   * The real constructor. Create a FileViewer object to display the specified
   * file from the specified directory
   */
  public FileViewer(String directory, String filename) {
    super(); // Create the frame

    // Destroy the window when the user requests it
    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        dispose();
      }
    });

    // Create a TextArea to display the contents of the file in
    textarea = new TextArea("", 24, 80);
    textarea.setFont(new Font("MonoSpaced", Font.PLAIN, 12));
    textarea.setEditable(false);
    this.add("Center", textarea);

    // Create a bottom panel to hold a couple of buttons in
    Panel p = new Panel();
    p.setLayout(new FlowLayout(FlowLayout.RIGHT, 10, 5));
    this.add(p, "South");

    // Create the buttons and arrange to handle button clicks
    Font font = new Font("SansSerif", Font.BOLD, 14);
    Button openfile = new Button("Open File");
    Button close = new Button("Close");
    openfile.addActionListener(this);
    openfile.setActionCommand("open");
    openfile.setFont(font);
    close.addActionListener(this);
    close.setActionCommand("close");
    close.setFont(font);
    p.add(openfile);
    p.add(close);

    this.pack();

    // Figure out the directory, from filename or current dir, if necessary
    if (directory == null) {
      File f;
      if ((filename != null) && (f = new File(filename)).isAbsolute()) {
        directory = f.getParent();
        filename = f.getName();
      } else
        directory = System.getProperty("user.dir");
    }

    this.directory = directory; // Remember the directory, for FileDialog
    setFile(directory, filename); // Now load and display the file
  }

  /**
   * Load and display the specified file from the specified directory
   */
  public void setFile(String directory, String filename) {
    if ((filename == null) || (filename.length() == 0))
      return;
    File f;
    FileReader in = null;
    // Read and display the file contents. Since we're reading text, we
    // use a FileReader instead of a FileInputStream.
    try {
      f = new File(directory, filename); // Create a file object
      in = new FileReader(f); // And a char stream to read it
      char[] buffer = new char[4096]; // Read 4K characters at a time
      int len; // How many chars read each time
      textarea.setText(""); // Clear the text area
      while ((len = in.read(buffer)) != -1) { // Read a batch of chars
        String s = new String(buffer, 0, len); // Convert to a string
        textarea.append(s); // And display them
      }
      this.setTitle("FileViewer: " + filename); // Set the window title
      textarea.setCaretPosition(0); // Go to start of file
    }
    // Display messages if something goes wrong
    catch (IOException e) {
      textarea.setText(e.getClass().getName() + ": " + e.getMessage());
      this.setTitle("FileViewer: " + filename + ": I/O Exception");
    }
    // Always be sure to close the input stream!
    finally {
      try {
        if (in != null)
          in.close();
      } catch (IOException e) {
      }
    }
  }

  /**
   * Handle button clicks
   */
  public void actionPerformed(ActionEvent e) {
    String cmd = e.getActionCommand();
    if (cmd.equals("open")) { // If user clicked "Open" button
      // Create a file dialog box to prompt for a new file to display
      FileDialog f = new FileDialog(this, "Open File", FileDialog.LOAD);
      f.setDirectory(directory); // Set the default directory

      // Display the dialog and wait for the user's response
      f.show();

      directory = f.getDirectory(); // Remember new default directory
      setFile(directory, f.getFile()); // Load and display selection
      f.dispose(); // Get rid of the dialog box
    } else if (cmd.equals("close")) // If user clicked "Close" button
      this.dispose(); // then close the window
  }

  /**
   * The FileViewer can be used by other classes, or it can be used standalone
   * with this main() method.
   */
  static public void main(String[] args) throws IOException {
    // Create a FileViewer object
    Frame f = new FileViewer((args.length == 1) ? args[0] : null);
    // Arrange to exit when the FileViewer window closes
    f.addWindowListener(new WindowAdapter() {
      public void windowClosed(WindowEvent e) {
        System.exit(0);
      }
    });
    // And pop the window up
    f.show();
  }
}

class TabListCellRenderer extends JLabel implements ListCellRenderer {
      protected static Border m_noFocusBorder = new EmptyBorder(1, 1, 1, 1);

      protected FontMetrics m_fm = null;

      public TabListCellRenderer() {
        super();
        setOpaque(true);
        setBorder(m_noFocusBorder);
      }

      public Component getListCellRendererComponent(JList list, Object value,
          int index, boolean isSelected, boolean cellHasFocus) {
        setText(value.toString());

        setBackground(isSelected ? list.getSelectionBackground() : list
            .getBackground());
        setForeground(isSelected ? list.getSelectionForeground() : list
            .getForeground());

        setFont(list.getFont());
        setBorder((cellHasFocus) ? UIManager
            .getBorder("List.focusCellHighlightBorder") : m_noFocusBorder);

        return this;
      }

      public void paint(Graphics g) {
        m_fm = g.getFontMetrics();

        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        getBorder().paintBorder(this, g, 0, 0, getWidth(), getHeight());

        g.setColor(getForeground());
        g.setFont(getFont());
        Insets insets = getInsets();
        int x = insets.left;
        int y = insets.top + m_fm.getAscent();

        StringTokenizer st = new StringTokenizer(getText(), "\t");
        while (st.hasMoreTokens()) {
          String str = st.nextToken();
          g.drawString(str, x, y);
          //insert distance for each tab
          x += m_fm.stringWidth(str) + 50;

          if (!st.hasMoreTokens())
            break;
        }
      }
}

class DirectoryInfo {
   
    String objectName;
    String sizeInBytes;
    String classOfService;
    String   lastModified;
   
    public String getObjectName() {
        return objectName;
    }
    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }
    public String getSizeInBytes() {
        return sizeInBytes;
    }
    public void setSizeInBytes(String sizeInBytes) {
        this.sizeInBytes = sizeInBytes;
    }
    public String getClassOfService() {
        return classOfService;
    }
    public void setClassOfService(String classOfService) {
        this.classOfService = classOfService;
    }
    public String getLastModified() {
        return lastModified;
    }
    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }
   
    public static String convertEpochToDate(String timeInMillis) {
       
        String date = new java.text.SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(new java.util.Date ((Long.parseLong(timeInMillis))*1000));
        return date;
    }
    @Override
    public String toString() {
        return ""+ objectName + "\t" + sizeInBytes + "\t"
                + classOfService + "\t" + lastModified;
    }
   
   
   
   
}

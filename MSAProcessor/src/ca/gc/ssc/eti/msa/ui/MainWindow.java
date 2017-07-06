package ca.gc.ssc.eti.msa.ui;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JFileChooser;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import java.awt.BorderLayout;

import javax.swing.JScrollPane;
import javax.swing.JDialog;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;

import ca.gc.ssc.eti.msa.Host;
import ca.gc.ssc.eti.msa.MSAProcessor;
import ca.gc.ssc.eti.msa.SMTPClient;
import ca.gc.ssc.eti.msa.SenderGroup;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;
import java.util.Set;
//import java.awt.Window.Type;
import java.awt.Color;

public class MainWindow {
	
	private JFrame frmMsaProcessor;
	JTextArea textArea;
	private JFileChooser fileChooser;
	private PartnerDialog partnerDialog;
	private JDialog progressDialog;
	private JProgressBar progressBar;
	private FileNameExtensionFilter csvFilter, txtFilter;
	private JMenu mnAction;
	
	private String fileName;
	private MSAProcessor processor;
	Set<SMTPClient> clients;
	Map<String,Host> hosts;
	private ByteArrayOutputStream out;
	
	
	class DNSWorker extends SwingWorker<Void, Void> {

		@Override
		protected Void doInBackground() throws Exception {
			processor.printDNSIssues(new PrintStream(out), hosts, progressBar);
			return null;
		}

		@Override
		protected void done() {
			progressDialog.setVisible(false);
			textArea.setText(out.toString());
			textArea.setCaretPosition(0);
			super.done();
		}
	}

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(
			        UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MainWindow window = new MainWindow();
					window.frmMsaProcessor.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public MainWindow() {
		initialize();
		fileChooser = new JFileChooser();
		fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir"))); //$NON-NLS-1$
		partnerDialog = new PartnerDialog(frmMsaProcessor);
		
		csvFilter = new FileNameExtensionFilter(Messages.getString("MainWindow.1"), "csv"); //$NON-NLS-1$ //$NON-NLS-2$
		txtFilter = new FileNameExtensionFilter(Messages.getString("MainWindow.3"), "txt"); //$NON-NLS-1$ //$NON-NLS-2$
		
		out = new ByteArrayOutputStream();
		
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmMsaProcessor = new JFrame();
		frmMsaProcessor.setTitle(Messages.getString("MainWindow.5")); //$NON-NLS-1$
		frmMsaProcessor.setBounds(100, 100, 1000, 600);
		frmMsaProcessor.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		progressDialog = new JDialog(frmMsaProcessor);
//		progressDialog.setType(Type.POPUP);
		progressDialog.setAlwaysOnTop(true);
		progressDialog.setResizable(false);
		progressDialog.setTitle(Messages.getString("MainWindow.6")); //$NON-NLS-1$
		progressDialog.setSize(200, 65);
		progressDialog.getContentPane().setLayout(new BorderLayout(0, 0));
		progressBar = new JProgressBar();
		progressBar.setStringPainted(true);
		progressBar.setForeground(Color.BLUE);
		progressDialog.getContentPane().add(progressBar, BorderLayout.CENTER);
		
		JMenuBar menuBar = new JMenuBar();
		frmMsaProcessor.getContentPane().add(menuBar, BorderLayout.NORTH);
		
		JMenu mnFile = new JMenu(Messages.getString("MainWindow.2")); //$NON-NLS-1$
		menuBar.add(mnFile);
		
		mnAction = new JMenu(Messages.getString("MainWindow.0")); //$NON-NLS-1$
		mnAction.setEnabled(false);
		menuBar.add(mnAction);
		
		JMenuItem mntmOpen = new JMenuItem(Messages.getString("MainWindow.9")); //$NON-NLS-1$
		mntmOpen.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				fileChooser.setFileFilter(csvFilter);
				int returnVal = fileChooser.showOpenDialog(frmMsaProcessor);
				if (returnVal != JFileChooser.APPROVE_OPTION)
					return;
				
				partnerDialog.setLocationRelativeTo(frmMsaProcessor);
				partnerDialog.setVisible(true);
				
				SenderGroup.reset();
				processor = new MSAProcessor();
				try {
					fileName = fileChooser.getSelectedFile().getCanonicalPath();
					out.reset();
					PrintStream ps = new PrintStream(out);
					// Parse the CSV file and build a Set of SMTPClient objects for each row
					clients = processor.parseFile(ps, fileName, partnerDialog.acronymE.getText(), partnerDialog.acronymF.getText());
					// Sort all of the clients, group them by host and determine the sender group for each host  
					hosts = processor.sortClients(clients);
					// Print a report of any hosts with conflicting Data Loss Prevention requirements 
					processor.printDLPConflicts(ps, hosts);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				mnAction.setEnabled(true);
				textArea.setText(out.toString());
				textArea.setCaretPosition(0);
			}
		});
		mnFile.add(mntmOpen);
		
		JMenuItem mntmSaveAs = new JMenuItem(Messages.getString("MainWindow.10")); //$NON-NLS-1$
		mntmSaveAs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				fileChooser.setFileFilter(txtFilter);
				if (fileName != null) {
					fileName = fileName.replaceAll("\\.csv$", ".txt"); //$NON-NLS-1$ //$NON-NLS-2$
					fileChooser.setSelectedFile(new File(fileName));
				}
				int returnVal = fileChooser.showSaveDialog(frmMsaProcessor);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					PrintStream ps = null;
					try {
						ps = new PrintStream(fileChooser.getSelectedFile());
						ps.print(out.toString());
						ps.close();
					} catch (FileNotFoundException e) {}
					finally {
						if (ps != null) {
							ps.close();
						}
					}
				}
			}
		});
		mnFile.add(mntmSaveAs);
		
		JMenuItem mntmExit = new JMenuItem(Messages.getString("MainWindow.13")); //$NON-NLS-1$
		mntmExit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				System.exit(0);
			}
		});
		mnFile.add(mntmExit);
		
		JMenuItem mntmCheckDns = new JMenuItem(Messages.getString("MainWindow.14")); //$NON-NLS-1$
		mntmCheckDns.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				out.reset();
				progressDialog.setLocationRelativeTo(frmMsaProcessor);
				progressDialog.setVisible(true);
				progressBar.setVisible(true);
				DNSWorker worker = new DNSWorker();
				worker.execute();
			}
		});
		mnAction.add(mntmCheckDns);
		
		JMenuItem mntmGenerateIronportConfig = new JMenuItem(Messages.getString("MainWindow.15")); //$NON-NLS-1$
		mntmGenerateIronportConfig.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				out.reset();
				PrintStream ps = new PrintStream(out);
				processor.printSenderGroups(ps, null);
				processor.printDictionary(ps, hosts, partnerDialog.acronymE.getText().toLowerCase(),
						partnerDialog.acronymF.getText().toLowerCase());
				textArea.setText(out.toString());
				textArea.setCaretPosition(0);
			}
		});
		mnAction.add(mntmGenerateIronportConfig);
		
		JMenuItem mntmGenerateMutlitenantIronport = new JMenuItem(Messages.getString("MainWindow.16")); //$NON-NLS-1$
		mntmGenerateMutlitenantIronport.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				out.reset();
				PrintStream ps = new PrintStream(out);
				processor.printSenderGroups(ps, partnerDialog.acronymE.getText().toUpperCase());
				processor.printDictionary(ps, hosts, partnerDialog.acronymE.getText().toLowerCase(),
						partnerDialog.acronymF.getText().toLowerCase());
				textArea.setText(out.toString());
				textArea.setCaretPosition(0);
			}
		});
		mnAction.add(mntmGenerateMutlitenantIronport);
		
		JPanel panel = new JPanel();
		frmMsaProcessor.getContentPane().add(panel, BorderLayout.CENTER);
		panel.setLayout(new BorderLayout(0, 0));
		
		textArea = new JTextArea();
		
		JScrollPane scrollPane = new JScrollPane(textArea);
				panel.add(scrollPane, BorderLayout.CENTER);
	}
}

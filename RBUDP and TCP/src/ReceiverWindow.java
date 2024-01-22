
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;

/**
 *
 * @author Keaglox
 */
public class ReceiverWindow extends javax.swing.JFrame {

    /**
     * Creates new form ReceiverWindow
     */
    public ReceiverWindow() {
        initComponents();
        fileReceivedLabel.setVisible(false);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        fileReceivedLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();
        jLabel2 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel1.setText("Receiver");

        fileReceivedLabel.setFont(new java.awt.Font("Segoe UI", 0, 20)); // NOI18N
        fileReceivedLabel.setText("File Received!");

        progressBar.setPreferredSize(new java.awt.Dimension(175, 15));

        jLabel2.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel2.setText("Waiting to receive file...");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(169, 169, 169)
                        .addComponent(jLabel1))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(158, 158, 158)
                        .addComponent(fileReceivedLabel))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(90, 90, 90)
                        .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(148, 148, 148)
                        .addComponent(jLabel2)))
                .addContainerGap(104, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jLabel1)
                .addGap(18, 18, 18)
                .addComponent(fileReceivedLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addContainerGap(10, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
    
//    int port = Integer.parseInt(portTextField.getText());
//    String fileSaveDirectory = "./";
//    try {
//        receiveAndCreate(port, fileSaveDirectory);
//    } catch (IOException e) {
//        e.printStackTrace();
//    }
//
//    fileReceivedLabel.setVisible(true);
    
    public static void receiveAndCreate(int port, String fileSaveDirectory) throws IOException {
        // Create socket，set the address and create the file to send
        DatagramSocket socket = new DatagramSocket(port);
        InetAddress address;
        byte[] receiveFileName = new byte[1024]; // Stores the data of the datagram which contains the file name
        DatagramPacket receiveFileNamePacket = new DatagramPacket(receiveFileName, receiveFileName.length);
        socket.receive(receiveFileNamePacket); // Receive the datagram with the file name

        System.out.println("Received file name.");
        byte[] data = receiveFileNamePacket.getData();
        String fileName = new String(data, 0, receiveFileNamePacket.getLength());

        File file = new File(fileSaveDirectory + "\\" + fileName);
        FileOutputStream outputFile = new FileOutputStream(file); // The stream through which we write the file content

        byte[] receiveFileSize = new byte[1024];
        DatagramPacket receiveFilesizePacket = new DatagramPacket(receiveFileSize, receiveFileSize.length);
        socket.receive(receiveFilesizePacket);
        
        byte[] fileSize_data = receiveFilesizePacket.getData();
        ByteBuffer bb = ByteBuffer.allocate(1024);
        bb.put(fileSize_data);
        bb.rewind();
        int fileSize = bb.getInt();
        System.out.println("FILE SIZE = " + fileSize);

        // Create a flag to indicate the last message
        boolean lastMessageFlag = false;
		
        // Store sequence number
        int sequenceNumber = 0;
        int lastSequenceNumber = 0;
        progressBar.setStringPainted(true);
        progressBar.setMaximum(fileSize);
        
        while (!lastMessageFlag) {
            // Create byte array for full message and another for file data without header
            byte[] message = new byte[1024];
            byte[] fileByteArray = new byte[1021];

            // Receive packet and retrieve message data
            DatagramPacket receivedPacket = new DatagramPacket(message, message.length);
            socket.setSoTimeout(0);
            socket.receive(receivedPacket);
            message = receivedPacket.getData();
            
            // Get the port and address for sending confirmation
            address = receivedPacket.getAddress();
            port = receivedPacket.getPort();

            // Retrieve sequence number
            sequenceNumber = ((message[0] & 0xff) << 8) + (message[1] & 0xff);

            // Retrieve the last message flag
            if ((message[2] & 0xff) == 1) {
                lastMessageFlag = true;
            } else {
                lastMessageFlag = false;
            }

            if (sequenceNumber == (lastSequenceNumber + 1)) {

                // Update the latest sequence number
                lastSequenceNumber = sequenceNumber;

                // Retrieve data from message
                for (int i=3; i < 1024 ; i++) {
                    fileByteArray[i-3] = message[i];
                }

                // Write message to file
                outputFile.write(fileByteArray);
                System.out.println("Received: Sequence number = " + sequenceNumber +", Flag = " + lastMessageFlag);
                progressBar.setValue(sequenceNumber);
                
                // Send ack
                sendAck(lastSequenceNumber, socket, address, port);

                // Check the last message
                if (lastMessageFlag) {
                    outputFile.close();
                } 
            } else {
                // If the packet has been received, send an acknowledgment of the packet again
                if (sequenceNumber < (lastSequenceNumber + 1)) {
                    // Send ack to received data packet
                    sendAck(sequenceNumber, socket, address, port);
                } else {
                    // Resend the ack of the last received packet
                    sendAck(lastSequenceNumber, socket, address, port);
                }
            }
        }
        
        socket.close();
        System.out.println("File " + fileName + " has been received.");
    }

    public static void sendAck(int lastSequenceNumber, DatagramSocket socket, InetAddress address, int port) throws IOException {
        byte[] ackPacket = new byte[2];
        ackPacket[0] = (byte)(lastSequenceNumber >> 8);
        ackPacket[1] = (byte)(lastSequenceNumber);
        DatagramPacket acknowledgement = new  DatagramPacket(ackPacket, ackPacket.length, address, port);
        socket.send(acknowledgement);
        System.out.println("Sent ack: Sequence Number = " + lastSequenceNumber);
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(ReceiverWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(ReceiverWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(ReceiverWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ReceiverWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new ReceiverWindow().setVisible(true);
            }
        });
        String fileSaveDirectory = "./";
        try {
            receiveAndCreate(5000, fileSaveDirectory);
            fileReceivedLabel.setVisible(true);
  
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private static javax.swing.JLabel fileReceivedLabel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private static javax.swing.JProgressBar progressBar;
    // End of variables declaration//GEN-END:variables
}
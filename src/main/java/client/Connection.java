package client;

import com.sun.org.apache.xpath.internal.SourceTree;

import java.io.*;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

public class Connection implements Runnable{

    private String ip;
    private int port;
    private String fileName;
    private long timeStart;
    private long momentTimeStart;
    private long momentTimeEnd;
    private long sentBytes;
    private long momentSentBytes;
    private long fileSize;

    public Connection(String ip, int port) {
        this.ip = ip;
        this.port = port;
        this.timeStart = 0;
        this.sentBytes = 0;
    }

    public void sendFile(String fileName) {
        this.fileName = fileName;
        Thread thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {
        try {
            //initializing socket and in/out streams
            Socket socket = new Socket(ip, port);
            DataOutputStream outData = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            ObjectOutputStream outObj = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream inObj = new ObjectInputStream(socket.getInputStream());

            //open file to transmit
            File file = new File(fileName);
            FileInputStream fileInputStream = new FileInputStream(fileName);

            //send meta-info about file to server
            this.fileSize = file.length();
            outObj.writeObject(new String[] {
                    file.getName(),
                    String.valueOf(this.fileSize)
            });

            //confirm that server received meta-info
            String answer = (String)inObj.readObject();
            if (!answer.equals("ok")) return;

            int count;
            int delayCounter = 0;
            long localTimeStart = 0;
            long localSentBytes = 0;
            byte[] buffer = new byte[8000];

            // timer for printing speed and progress
            Timer timer = new Timer(true);
            timer.schedule(new SpeedMeasureTimerTask(),3000,3000);

            //start sending file
            timeStart = System.currentTimeMillis();
            while ((count = fileInputStream.read(buffer)) > 0) {
                if (delayCounter == 0) {
                    localTimeStart = System.currentTimeMillis();
                    localSentBytes = 0;
                }
                outData.write(buffer, 0, count);
                outData.flush();
                sentBytes += count;
                localSentBytes += count;
                delayCounter++;
                if (delayCounter > 30) { //otherwise momentum speed might be infinite because one loop iteration ends in less than in 1 millisecond
                    momentTimeEnd = System.currentTimeMillis();
                    momentSentBytes = localSentBytes;
                    momentTimeStart = localTimeStart;
                    delayCounter = 0;
                }
            }
            long totalTime = System.currentTimeMillis() - timeStart;

            //finishing transmission process
            socket.shutdownOutput();
            timer.cancel();

            //confirm that server received file without errors
            answer = (String)inObj.readObject();
            if (answer.equals("success")) {
                totalTime /= 1000;
                long m = totalTime / 60;
                long s = totalTime - m * 60;
                System.out.println("File sent successfully. Transmission time: " + m + "m " + s + "s");
            }
            inObj.close();
            socket.close();
        } catch (FileNotFoundException e) {
            System.out.println("No such file: " + fileName);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private class SpeedMeasureTimerTask extends TimerTask {

        @Override
        public void run() {
            System.out.println("--------------------------------");
            System.out.println("Progress: " + (double)sentBytes / fileSize * 100 + "% ");
            System.out.println("Average transmission speed: " + (((double) sentBytes) / 1024 / (System.currentTimeMillis() - timeStart) * 1000) + " KBs per second");
            System.out.println("Moment transmission speed: " + (((double) momentSentBytes) / 1024 / (momentTimeEnd - momentTimeStart) * 1000) + " KBs per second");
        }
    }
}

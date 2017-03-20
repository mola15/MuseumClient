package it.polimi.molinaroli.museumclient.Logic;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;



/**
 * The server creates the sockets and waits for messages form the clients,
 * handles the request and responds.
 */
public class Server {
    private ServerSocket mServerSocket;
    private int mLocalPort;
    private ArrayList<Socket> clients;
    private Context context;
    String mCurrentPhotoPath;


    public Server(Context c){
        this.context = c;
        try {
            initializeServerSocket();
        } catch (IOException e) {
            Log.e("Server","Error server not started");
            e.printStackTrace();
        }
    }

    /**
     * starts the serversocket and stores the port
     * @throws IOException
     */
    public void initializeServerSocket() throws IOException {
        // Initialize a server socket on the next available port.
        setmServerSocket(new ServerSocket(0));
        // Store the chosen port.
        setmLocalPort(getmServerSocket().getLocalPort());
        clients = new ArrayList<>();
        Log.d("server","server port: "+ getmLocalPort());
    }
    public void startServer(){

        try {
            while(true) {
                Log.d("server" ,"server started");
                // bloccante finchè non avviene una connessione:
                Socket clientSocket = getmServerSocket().accept();
                //salva i client connessi
                getClients().add(clientSocket);
                Log.d("server","Connection accepted: "+ clientSocket);
                try {
                    new ServerThread(clientSocket);
                } catch(IOException e) {
                    clientSocket.close();
                }
            }
        }
        catch (IOException e) {
            System.err.println("Accept failed");
            System.exit(1);
        }
        //code to be executed only after a failure
        System.out.println("EchoMultiServer: closing...");
        try {
            getmServerSocket().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ServerSocket getmServerSocket() {
        return mServerSocket;
    }

    public void setmServerSocket(ServerSocket mServerSocket) {
        this.mServerSocket = mServerSocket;
    }

    public int getmLocalPort() {
        return mLocalPort;
    }

    public void setmLocalPort(int mLocalPort) {
        this.mLocalPort = mLocalPort;
    }

    public ArrayList<Socket> getClients() {
        return clients;
    }

    public void setClients(ArrayList<Socket> clients) {
        this.clients = clients;
    }

    /**
     * Server thread that it used to communicate with a single client
     * in that thread will be implemented communications methods
     * there will be one thread for each client
     */
    class ServerThread extends Thread {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        public ServerThread(Socket s) throws IOException {
            socket = s;
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            OutputStreamWriter osw = new OutputStreamWriter(socket.getOutputStream());
            out = new PrintWriter(new BufferedWriter(osw), true);
            start();
            Log.d("Server","ServerThread started" + s.toString());
        }
        public void run() {
            Context c = context;
            try {
                while (true) {
                    //legge dal buffer in ingresso (legge dal client)
                    String str = in.readLine();
                    Log.d("Server",str);
                    if (str.equals("END")) break; //esce dal while
                    else if (str.equals("URL")){
                        String url = in.readLine();
                        out.println("url letta");
                        Intent viewIntent = new Intent();
                        viewIntent.setAction(Intent.ACTION_VIEW);
                        viewIntent.setData(Uri.parse(url));
                        viewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        // Verify that the intent will resolve to an activity
                        if (viewIntent.resolveActivity(c.getPackageManager()) != null) {
                            c.startActivity(viewIntent);
                        } else{
                            Log.d("server","errore nell apertura del browser");
                        }

                    }else if (str.equals("INTENT")){
                        String json = in.readLine();
                        Log.d("server",json);
                        try {
                            JSONObject job = new JSONObject(json);
                            Intent i = IntentConverter.JSONToIntent(job);
                            //ho rigenerato l'intento controllo che tipo è
                            try{
                               String tipo = i.getStringExtra("andorid.intent.extra.LIQUIDMETHOD");
                                if ( tipo.equals("BROADCAST")){
                                    if (i.getAction().equals(Intent.ACTION_MEDIA_BUTTON)){

                                        int code = i.getIntExtra("android.intent.extra.KEY_EVENT_CODE",0);
                                        i.putExtra(Intent.EXTRA_KEY_EVENT,new KeyEvent(KeyEvent.ACTION_DOWN, code));
                                        context.sendOrderedBroadcast(i, null);
                                        Log.e("server","broadcast lanciato");
                                        i = new Intent(Intent.ACTION_MEDIA_BUTTON);
                                        i.putExtra(Intent.EXTRA_KEY_EVENT,new KeyEvent(KeyEvent.ACTION_UP, code));
                                        context.sendOrderedBroadcast(i, null);
                                        Log.e("server","broadcast lanciato");
                                    }
                                }
                            }catch(Exception e){
                            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            out.println("intento rigenerato");
                            Log.e("server",IntentConverter.intentToJSON(i).toString());
                            //context.startActivity(i);
                            String title = "Open with:";
                            // Create intent to show the chooser dialog
                            Intent chooser = Intent.createChooser(i, title);
                            chooser.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                context.startActivity(chooser);}

                        } catch (JSONException e) {
                            Log.e("server","impossibile costruire il json");
                            e.printStackTrace();
                        }
                    }

                }

               //cose da farlgi fare in uscita
                Log.d("Server","ServerThread closing...");
            } catch (IOException e) {}
            try {
                socket.close();
            } catch(IOException e) {}
        }
    } // ServerThread

    public void stopServer(){
        try {
            mServerSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

        private File createImageFile() throws IOException {
            // Create an image file name
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String imageFileName = "JPEG_" + timeStamp + "_";
            File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File image = File.createTempFile(
                    imageFileName,  /* prefix */
                    ".jpg",         /* suffix */
                    storageDir      /* directory */
            );

            // Save a file: path for use with ACTION_VIEW intents
            mCurrentPhotoPath = image.getAbsolutePath();
            return image;
        }
}

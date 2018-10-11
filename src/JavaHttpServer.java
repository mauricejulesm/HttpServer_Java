import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;

public final class JavaHttpServer {
    public static void main(String argv[]) throws Exception {
        // port number.
        int port = 9090;

        // Establish the listen socket.
        ServerSocket serverSocket = new ServerSocket(port);

        //HTTP service requests in an infinite loop.
        while (true) {
            // Listen for a TCP connection request.
            Socket connectionSocket = serverSocket.accept();

            // process HTTP request message.
            HttpRequest request = new HttpRequest(connectionSocket);

            // new thread to process the request.
            Thread thread = new Thread(request);

            // Start the thread.
            thread.start();
        }
    }
}


final class HttpRequest implements Runnable {
    private final static String CRLF = "\r\n";//returning carriage return (CR) and a line feed (LF)
    private Socket socket;

    HttpRequest(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try {
            processRequest();
        } catch (Exception e) {
//            System.out.println(e);
        }
    }

    private void processRequest() throws Exception {

        InputStream inStream = socket.getInputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inStream));

        String requestLine = bufferedReader.readLine();// get /path/file.html version of http

        // Display the request line.
        System.out.println();
        System.out.println("Request Line: "+requestLine);

        // Extract the filename from the request line.
        StringTokenizer tokens = new StringTokenizer(requestLine);
        tokens.nextToken();
        String fileName = tokens.nextToken();

        // Prepend a "." so that file request is within the current directory.
        fileName = "." + fileName;

        //Open the requested file.
        FileInputStream fis = null;
        boolean fileExists = true;
        try {
            fis = new FileInputStream(fileName);
        } catch (FileNotFoundException e) {
            fileExists = false;
        }

        //Construct the response message.
        String statusLine;
        String contentTypeLine;
        String entityBody = null;

        if (fileExists) {
            statusLine = "HTTP/1.0 200 OK" + CRLF; //success message
            contentTypeLine = "Content-type: " + contentType(fileName) + CRLF;
        }//content info

        else {
            statusLine = "HTTP/1.0 404 File Not Found" + CRLF;//error message
            contentTypeLine = "Content-type: " + "text/html" + CRLF;//
            entityBody = "<html>" +
                            "<head>" +
                                "<title>File Not Found</title>" +
                            "</head>" +
                                "<body>" +
                                    "<p>The file you are looking for is not available</p>" +
                            "</body>" +
                        "</html>";
        }

        dataOutputStream.writeBytes(statusLine);
        dataOutputStream.writeBytes(contentTypeLine);

        //blank line to indicate the end of the header lines.
        dataOutputStream.writeBytes(CRLF);


        //Send the entity body.
        if (fileExists) {
            sendBytes(fis, dataOutputStream);
            dataOutputStream.writeBytes(statusLine);
            dataOutputStream.writeBytes(contentTypeLine);
            fis.close();
        } else {
            dataOutputStream.writeBytes(statusLine);
            dataOutputStream.writeBytes(entityBody);
            dataOutputStream.writeBytes(contentTypeLine);
        }


        System.out.println("*****");
        System.out.println("File Name: " +fileName);
        System.out.println("*****");

        // Get and display the header lines.
        String headerLine;
        while ((headerLine = bufferedReader.readLine()).length() != 0) {
            System.out.println(headerLine);
        }

        // Close streams and socket.
        dataOutputStream.close();
        bufferedReader.close();
        socket.close();
    }

    //return the file types
    private static String contentType(String fileName) {
        if (fileName.endsWith(".htm") || fileName.endsWith(".html")) {
            return "text/html";
        }
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (fileName.endsWith(".gif")) {
            return "image/gif";
        }
//        return "application/octet-stream";  this line will make the browser download and save file!
        return "File type is something else!";
    }


    //set up input output streams
    private static void sendBytes(FileInputStream fis, OutputStream os) throws Exception {
        // Construct a 1K buffer to hold bytes on their way to the socket.
        byte[] buffer = new byte[1024];
        int bytes;

        // Copy requested file into the socket's output stream.
        while ((bytes = fis.read(buffer)) != -1) {
            os.write(buffer, 0, bytes);
        }
    }
}


# Spring sftp file streaming listener example
This app will stream files from an SFTP server at localhost, keep them in memory (no local filesystem use), then delete the remote file when it's done processing it.

## To Run this app
`./gradlew bootRun`

## To test
run an sftp server at localhost with user 'foo' password 'pass' with directory /home/foo/upload.  For example:

`docker run -p 22:22 -d atmoz/sftp foo:pass:::upload`

Use an sftp client to authenticate with 'foo' user and push files to ~/upload.  You should see the spring app print the file payload and headers.

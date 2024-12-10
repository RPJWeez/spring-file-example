package com.example.fileexample;

import java.io.InputStream;

import org.apache.sshd.sftp.client.SftpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.file.filters.AcceptAllFileListFilter;
import org.springframework.integration.file.filters.ChainFileListFilter;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.RegexPatternFileListFilter;
import org.springframework.integration.file.filters.SimplePatternFileListFilter;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.handler.advice.ExpressionEvaluatingRequestHandlerAdvice;
import org.springframework.integration.sftp.inbound.SftpStreamingMessageSource;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;
import org.springframework.integration.transformer.StreamTransformer;
import org.springframework.messaging.MessageHandler;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SftpConfig {

    private final FileService fileService;

    @Bean
    public SessionFactory<SftpClient.DirEntry> sftpSessionFactory() {
        DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory(true);
        factory.setHost("localhost");
        factory.setPort(22);
        factory.setUser("foo");
        factory.setPassword("pass");
        factory.setAllowUnknownKeys(true);
        return new CachingSessionFactory<>(factory);
    }

    @Bean
    @InboundChannelAdapter(channel = "stream")
    public MessageSource<InputStream> ftpMessageSource() {
        SftpStreamingMessageSource messageSource = new SftpStreamingMessageSource(template());
        messageSource.setRemoteDirectory("upload/");

        //this filter will stream all files in the directory regardless of whether or not this application has already processed it
        messageSource.setFilter(new AcceptAllFileListFilter<>());
        // messageSource.setFilter(filter);
        messageSource.setMaxFetchSize(1);
        return messageSource;
    }

    @Bean
    @Transformer(inputChannel = "stream", outputChannel = "data")
    public org.springframework.integration.transformer.Transformer transformer() {
        return new StreamTransformer("UTF-8");
    }

    @Bean
    public SftpRemoteFileTemplate template() {
        return new SftpRemoteFileTemplate(sftpSessionFactory());
    }

    // This tells Spring to send all files detected on the remote location to the
    // FileService handleFile method.
    @ServiceActivator(inputChannel = "data", adviceChain = {"after"})
    @Bean
    public MessageHandler handle() {
        return fileService::handleFile;
    }

    // What to do after handling a new file
    @Bean
    public ExpressionEvaluatingRequestHandlerAdvice after() {
        ExpressionEvaluatingRequestHandlerAdvice advice = new ExpressionEvaluatingRequestHandlerAdvice();

        // delete the remote file when it is successfully processed
        advice.setOnSuccessExpressionString(
                "@template.remove(headers['file_remoteDirectory'] + '/' +  headers['file_remoteFile'])");

        // delete the remote file when processing fails
        advice.setOnFailureExpressionString(
            "@template.remove(headers['file_remoteDirectory'] + '/' +  headers['file_remoteFile'])");

        advice.setPropagateEvaluationFailures(true);
        return advice;
    }
}

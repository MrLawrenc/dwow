package com.github.mrlawrenc.attach.write.impl;

import com.github.mrlawrenc.attach.write.AbstractWriter;
import com.github.mrlawrenc.attach.write.Writeable;
import com.github.mrlawrenc.attach.write.WriterResp;
import com.github.mrlawrenc.attach.write.entity.HttpWriterResp;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * @author : MrLawrenc
 * date  2020/7/6 21:34
 * <p>
 * 将内容输出到http流
 */
@Slf4j
public class HttpWriter extends AbstractWriter<String> {
    private static final String DEFAULT_METHOD = "POST";
    private HttpURLConnection connection;

    @Override
    protected void init(String url) {
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(5000);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestMethod("POST");
        } catch (IOException e) {
            log.error("create http connection error", e);
        }
    }

    @Override
    public void destroy() {
        connection.disconnect();
    }

    @Override
    public WriterResp write(Writeable writeable) {
        return new HttpWriterResp();
    }

    public void postWrite(String json) {
        json = Objects.isNull(json) ? "{\"key\":1}" : json;
        try (OutputStream os = connection.getOutputStream()) {
            os.write(json.getBytes(StandardCharsets.UTF_8.displayName()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        try (InputStream in = new BufferedInputStream(connection.getInputStream())) {
            byte[] bytes = new byte[in.available()];
            in.read(bytes);
            System.out.println(new String(bytes));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
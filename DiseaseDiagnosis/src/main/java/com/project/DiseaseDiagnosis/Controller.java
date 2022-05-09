package com.project.DiseaseDiagnosis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;

@CrossOrigin(maxAge = 3600)
@RestController
public class Controller {
private static final Logger logger = LoggerFactory.getLogger(Controller.class);

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String uploadFile(@RequestParam MultipartFile file) throws IOException, InterruptedException {
        logger.info(String.format("File name '%s' uploaded successfully.", file.getOriginalFilename()));
        InputStream initialStream = file.getInputStream();
        byte[] buffer = new byte[initialStream.available()];
        initialStream.read(buffer);
        Main main = new Main();
        File targetFile = new File("src/main/resources/static/dna.txt");
        try (OutputStream outStream = new FileOutputStream(targetFile)) {
            outStream.write(buffer);
        }
        String data = main.mainFunc(targetFile);
        return data;
    }
}

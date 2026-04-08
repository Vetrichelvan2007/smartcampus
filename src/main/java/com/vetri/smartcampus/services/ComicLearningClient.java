package com.vetri.smartcampus.services;

import com.vetri.smartcampus.models.student.ComicLearningRequest;

import java.io.IOException;
import java.io.OutputStream;

public interface ComicLearningClient {

    void streamComic(ComicLearningRequest request, OutputStream outputStream) throws IOException, InterruptedException;
}

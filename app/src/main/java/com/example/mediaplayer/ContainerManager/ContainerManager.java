package com.example.mediaplayer.ContainerManager;

import com.example.mediaplayer.ContainerManager.Decoder.Decoder;
import com.example.mediaplayer.ContainerManager.Parser.Parser;
import com.example.mediaplayer.ContainerManager.Parser.WavParser.WavFileException;
import com.example.mediaplayer.Data.Container.Container;

import java.io.File;
import java.io.IOException;

public class ContainerManager {
    Container container;
    Parser parser;
    Decoder decoder;

    public ContainerManager(Container container) {
        this.container = container;
    }
    public Container getContainer(){
        return container;
    }

    public void Decode(){ }
    public void Parse() throws IOException, WavFileException {
        parser.parse();
    }
    public void StartManaging(){}
}

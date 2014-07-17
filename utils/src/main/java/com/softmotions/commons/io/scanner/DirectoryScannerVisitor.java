package com.softmotions.commons.io.scanner;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public interface DirectoryScannerVisitor {

    void visit(Path file, BasicFileAttributes attrs) throws IOException;

    void error(Path file, IOException exc) throws IOException;

}

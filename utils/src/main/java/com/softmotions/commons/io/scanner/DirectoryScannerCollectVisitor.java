package com.softmotions.commons.io.scanner;

import com.softmotions.commons.cont.Pair;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class DirectoryScannerCollectVisitor implements DirectoryScannerVisitor {

    final List<Pair<Path, BasicFileAttributes>> matches = new ArrayList<>();

    final List<Pair<Path, IOException>> errors = new ArrayList<>();


    @Override
    public void visit(Path file, BasicFileAttributes attrs) throws IOException {
        matches.add(new Pair<>(file, attrs));
    }

    @Override
    public void error(Path file, IOException exc) throws IOException {
        errors.add(new Pair<>(file, exc));
    }

    public List<Pair<Path, BasicFileAttributes>> getMatches() {
        return matches;
    }

    public List<Pair<Path, IOException>> getErrors() {
        return errors;
    }

    public void clear() {
        matches.clear();
        errors.clear();
    }
}

/*
 *Vaturi Omri - 305744666
 * Tzach Itshak Ofir - 208062943
 */

import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.BitSet;

public class LZSSCompressor {
    private String outputFilePath;
    private File inputFilePath;
    private int windowSize;
    private int lookAheadSize;
    private int minMatch;
    private int paddingBits = 0;

    public LZSSCompressor(String inPath, String outPath, int windowSize, int lookAhead, int minMatch) {
        this.outputFilePath = outPath;
        this.inputFilePath = new File(inPath);
        this.windowSize = windowSize;
        this.lookAheadSize = lookAhead;
        this.minMatch = minMatch;
    }

    public void compress() throws IOException {
        byte[] source = Files.readAllBytes(this.inputFilePath.toPath());
        StringBuilder encodedData = initializeEncodedData(source, this.minMatch);
        StringBuilder searchBuffer = initializeSearchBuffer(source, this.minMatch);
        int sourcePosition = this.minMatch;
        performEncoding(source, this.windowSize, this.lookAheadSize, this.minMatch, encodedData, searchBuffer, sourcePosition);
        writeRedundantBits(encodedData);
        BitSet encodedBits = ConvertToBits(encodedData);
        writeToFile(encodedBits);
    }

    private StringBuilder initializeSearchBuffer(byte[] source, int minMatch) {
        StringBuilder searchBuffer = new StringBuilder();

        for (int i = 0; i < minMatch; i++) {
            searchBuffer.append((char) (Byte.toUnsignedInt(source[i])));
        }

        return searchBuffer;
    }

    private StringBuilder initializeEncodedData(byte[] source, int minMatch) {
        StringBuilder encodedData = new StringBuilder();
        String windowSizeBits = byteToBinaryString(LogBaseTwo(this.windowSize));
        String maxMatchBits = byteToBinaryString(LogBaseTwo(this.lookAheadSize));

        encodedData.append(windowSizeBits);
        encodedData.append(maxMatchBits);

        encodeCharacter(minMatch, encodedData, source, 0);

        return encodedData;
    }

    private int LogBaseTwo(int parameter) {
        return (int) (Math.log(parameter) / Math.log(2));
    }

    public void performEncoding(byte[] source, int windowSize, int maxMatch, int minMatch, StringBuilder encodedData, StringBuilder searchBuffer, int sourcePosition) {
        PatternMatch match = new PatternMatch();
        while (sourcePosition < source.length) {
            searchForMatch(source, match, searchBuffer, sourcePosition, maxMatch);
            if (match.getLength() > minMatch) {
                encodeMatch(match, encodedData);
            } else {
                encodeCharacter(match.getLength(), encodedData, source, sourcePosition);
            }
            sourcePosition = sourcePosition + match.getLength();
            updateSearchBuffer(match, searchBuffer, windowSize);
        }
    }

    private void searchForMatch(byte[] source, PatternMatch match, StringBuilder searchBuffer, int sourcePosition, int maxMatch) {
        match.reset();
        byte[] dataChunk = Arrays.copyOfRange(source, sourcePosition, sourcePosition + maxMatch);
        int dataChunkOffset = 0;
        while (match.getValue().length() < maxMatch - 1) {
            char nextChar = (char) (Byte.toUnsignedInt(dataChunk[dataChunkOffset]));

            if (searchBuffer.toString().contains(match.getValue() + nextChar)) {
                match.addByte(dataChunk[dataChunkOffset]);
                int tmpOffSet = searchBuffer.indexOf(match.getValue());
                match.setOffset(tmpOffSet);
                match.incLength();
                dataChunkOffset++;
            } else {
                if (match.getLength() == 0) {
                    match.setLength(1);
                    match.addByte(source[sourcePosition]);
                }
                return;
            }
        }
    }

    private void encodeMatch(PatternMatch match, StringBuilder encodedData) {
        encodedData.append('1');

        int MAX_BYTE_VALUE = 255;
        if (match.getOffset() > MAX_BYTE_VALUE)
            encodedData.append('1');
        else
            encodedData.append('0');
        encodedData.append(byteToBinaryString(match.getOffset()));

        if (match.getLength() > MAX_BYTE_VALUE)
            encodedData.append('1');
        else
            encodedData.append('0');
        encodedData.append(byteToBinaryString(match.getLength()));
    }

    private void encodeCharacter(int matchLength, StringBuilder encodedData, byte[] source, int sourcePosition) {
        for (int i = 0; i < matchLength; i++) {
            encodedData.append('0');
            int byteAsUnsignedInt = Byte.toUnsignedInt((source[sourcePosition]));
            encodedData.append(byteToBinaryString(byteAsUnsignedInt));
            sourcePosition++;
        }
    }

    private void updateSearchBuffer(PatternMatch match, StringBuilder searchBuffer, int windowSize) {
        for (int i = 0; i < match.getLength(); i++) {
            if (searchBuffer.length() >= windowSize)
                searchBuffer.deleteCharAt(0);
            searchBuffer.append(match.getValue().charAt(i));
        }
    }

    private void writeRedundantBits(StringBuilder encodedData) {
        this.paddingBits = 64 - ((encodedData.length() + 8) % 64);
        encodedData.insert(0, byteToBinaryString(this.paddingBits));
    }

    private BitSet ConvertToBits(StringBuilder encodedData) {
        BitSet encodedBits = new BitSet(encodedData.length());
        int nextIndexOfOne = encodedData.indexOf("1", 0);
        while (nextIndexOfOne != -1) {
            encodedBits.set(nextIndexOfOne);
            nextIndexOfOne++;
            nextIndexOfOne = encodedData.indexOf("1", nextIndexOfOne);
        }
        return encodedBits;
    }

    private void writeToFile(BitSet encodedBits) throws IOException {
        FileOutputStream writer = new FileOutputStream(this.outputFilePath);
        ObjectOutputStream objectWriter = new ObjectOutputStream(writer);
        objectWriter.writeObject(encodedBits);
        objectWriter.close();
    }

    private String byteToBinaryString(int parameter) {
        StringBuilder binaryRepresentation = new StringBuilder(Integer.toBinaryString(parameter));

        while (binaryRepresentation.length() % 8 != 0) {
            binaryRepresentation.insert(0, '0');
        }

        return binaryRepresentation.toString();
    }
}
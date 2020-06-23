package com.example.mediaplayer.ContainerManager.Parser;



import com.example.mediaplayer.Data.Container.Container;
import com.example.mediaplayer.Data.Container.Stco;
import com.example.mediaplayer.Data.Container.Stsc;
import com.example.mediaplayer.Data.Container.Stsz;
import com.example.mediaplayer.Data.Container.Trak;
import com.example.mediaplayer.Data.Container.TrakFormat;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class MB4Parser extends Parser {

    private byte[] mAllBytes;
    private FileInputStream in;


    public MB4Parser(Container container) {
        super(container);

        try {
            int size = in.available();
            mAllBytes = new byte[size];

            BufferedInputStream buf = new BufferedInputStream(in);
            buf.read(mAllBytes, 0, mAllBytes.length);
            buf.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


    }

    @Override
    public void Parse() {
        TrakBuilder builder = new TrakBuilder();
        Trak trak1 = builder.buildTraks(0);
        Trak trak2 = builder.buildTraks(1);
    }

    private class TrakBuilder {

        private BoxAnalyzer mAnalyzer;
        private int mCurrentPosition;
        private int mTrakDuration;
        private int mModificationTime;
        private int mTrakId;
        private TrakFormat mFormat;
        private int mCreattionTime;
        private byte[] trakData;

        TrakBuilder() {
            mAnalyzer = new BoxAnalyzer();
            mCurrentPosition = 0;
        }

        private Trak buildTraks(int trakNumber) {
            Stco stco;
            Stsz stsz;
            Stsc stsc;

            mCurrentPosition = searchForBoxInSameLevel("moov", 0);
            mCurrentPosition = calculateBoxPositionAfterEntering("moov", mCurrentPosition);


            for (int i = 0; i <= trakNumber; i ++) {
                mCurrentPosition = searchForBoxInSameLevel("trak", mCurrentPosition);
            }
            mCurrentPosition = calculateBoxPositionAfterEntering("trak", mCurrentPosition);

            mCurrentPosition = calculateBoxPositionAfterEntering("tkhd", mCurrentPosition);
            mAnalyzer.readInfoFromTkhd();

            mCurrentPosition = calculateBoxPositionBeforeEntering("tkhd", mCurrentPosition);

            mCurrentPosition = searchForBoxInSameLevel("mdia", mCurrentPosition);
            mCurrentPosition = calculateBoxPositionAfterEntering("mdia", mCurrentPosition);

            mCurrentPosition = searchForBoxInSameLevel("hdlr", mCurrentPosition);
            mCurrentPosition = calculateBoxPositionAfterEntering("hdlr", mCurrentPosition);
            mAnalyzer.readInfoFromHdlr();

            mCurrentPosition = calculateBoxPositionBeforeEntering("hdlr", mCurrentPosition);

            mCurrentPosition = searchForBoxInSameLevel("minf", mCurrentPosition);
            mCurrentPosition = calculateBoxPositionAfterEntering("minf", mCurrentPosition);

            mCurrentPosition = searchForBoxInSameLevel("stbl", mCurrentPosition);
            mCurrentPosition = calculateBoxPositionAfterEntering("stbl", mCurrentPosition);


            mCurrentPosition = searchForBoxInSameLevel("stsc", mCurrentPosition);
            mCurrentPosition = calculateBoxPositionAfterEntering("stsc", mCurrentPosition);
            stsc = mAnalyzer.readInfoFromStsc();

            mCurrentPosition = calculateBoxPositionBeforeEntering("stsc", mCurrentPosition);

            mCurrentPosition = searchForBoxInSameLevel("stsz", mCurrentPosition);
            mCurrentPosition = calculateBoxPositionAfterEntering("stsz", mCurrentPosition);
            stsz = mAnalyzer.readInfoFromStsz();

            mCurrentPosition = calculateBoxPositionBeforeEntering("stsz", mCurrentPosition);

            mCurrentPosition = searchForBoxInSameLevel("stco", mCurrentPosition);
            mCurrentPosition = calculateBoxPositionAfterEntering("stco", mCurrentPosition);
            stco = mAnalyzer.readInfoFromStco();

            // ?? will return trak Raw Data
            trakData = mAnalyzer.extractTrakData(stco, stsz, stsc, trakNumber);
            return new Trak(mTrakDuration, mModificationTime, 0, mTrakId, true,
                    mFormat, mCreattionTime, trakData);
        }


        private int updateCurentPosition(int mCurrentPosition, int value, String boxType) {

            if (boxType != null) {
                if (isFullBox(boxType)) {
                    return mCurrentPosition += 12;
                } else {
                    return mCurrentPosition += 8;
                }
            }

            return mCurrentPosition += value;

        }

        private int updateSize(int position) {
            byte[] currentBytes = read4Bytes(position);
            return new BigInteger(currentBytes).intValue();
        }

        private String updateType(int position) {
            byte[] currentBytes = read4Bytes(position);
            return new String(currentBytes);
        }

        private boolean isFullBox(String type) {
            ArrayList<String> types = new ArrayList<>();
            types.add("mvhd");
            types.add("tkhd");
            types.add("stco");
            types.add("stsz");
            types.add("stsc");
            types.add("hdlr");

            if (types.contains(type)) {
                return true;
            }
            return false;

        }

        private int searchForBoxInSameLevel(String boxName, int startingPosition) {
            int boxSize = updateSize(startingPosition);
            String currentBoxName;
            int position = startingPosition;

            do {

                position = updateCurentPosition(position, boxSize, null);

                boxSize = updateSize(position);
                currentBoxName = updateType(position + 4);

                if (boxName.equals(currentBoxName)) {
                    return position;
                }
                if (position > mAllBytes.length) {
                    return -1;
                }

            } while (true);

        }

        private int calculateBoxPositionAfterEntering(String boxName, int mCurrentPosition) {
            if (isFullBox(boxName)) {
                return mCurrentPosition + 12;
            } else
                return mCurrentPosition + 8;

        }

        private int calculateBoxPositionBeforeEntering(String boxName, int mCurrentPosition) {
            if (isFullBox(boxName)) {
                return mCurrentPosition - 12;
            } else
                return mCurrentPosition - 8;
        }

        private byte[] read4Bytes(int position) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(mAllBytes[position]);
            out.write(mAllBytes[position + 1]);
            out.write(mAllBytes[position + 2]);
            out.write(mAllBytes[position + 3]);

            return out.toByteArray();
        }

        private class BoxAnalyzer {
            private Stco readInfoFromStco() {
                int entryCount = new BigInteger(read4Bytes(mCurrentPosition)).intValue();
                List<Integer> values = new ArrayList<>();

                for (int i = 0; i < entryCount; i ++) {
                    int value = new BigInteger(read4Bytes(mCurrentPosition + 4 + (i * 4))).intValue();
                    values.add(value);
                }

                return new Stco(entryCount, values);
            }

            private Stsc readInfoFromStsc() {

                int entryCount = new BigInteger(read4Bytes(mCurrentPosition)).intValue();
                List<Integer> firstChunkTable = new ArrayList<>();
                List<Integer> samplesPerChunkTable = new ArrayList<>();

                for (int i = 0; i < entryCount; i ++) {
                    int firstChunk = new BigInteger(read4Bytes(mCurrentPosition + 4 + (i * 12))).intValue();
                    int samplesPerChunk = new BigInteger(read4Bytes(mCurrentPosition + 8 + (i * 12))).intValue();
                    int sampleDescriptionIndex = new BigInteger(read4Bytes(mCurrentPosition + 12 +
                            (i * 12))).intValue();

                    firstChunkTable.add(firstChunk);
                    samplesPerChunkTable.add(samplesPerChunk);

                }

                return new Stsc(entryCount, firstChunkTable, samplesPerChunkTable);

            }

            private Stsz readInfoFromStsz() {
                int sampleSize = new BigInteger(read4Bytes(mCurrentPosition)).intValue();
                int sampleCount = new BigInteger(read4Bytes(mCurrentPosition + 4)).intValue();
                List<Integer> sampleSizeTable = new ArrayList<>();

                if (sampleSize == 0) {
                    for (int i = 0; i < sampleCount; i++) {
                        int oneSampleSize = new BigInteger(read4Bytes(mCurrentPosition + 8 + (i * 4))).intValue();
                        sampleSizeTable.add(oneSampleSize);

                    }
                }

                return new Stsz(sampleSize, sampleCount, sampleSizeTable);
            }

            private void readInfoFromTkhd() {
                mCreattionTime = new BigInteger(read4Bytes(mCurrentPosition)).intValue();
                mModificationTime = new BigInteger(read4Bytes(mCurrentPosition + 4)).intValue();
                mTrakId = new BigInteger(read4Bytes(mCurrentPosition + 8)).intValue();
                mTrakDuration = new BigInteger(read4Bytes(mCurrentPosition + 16)).intValue();
            }

            private void readInfoFromHdlr() {
                int preDefindd = new BigInteger(read4Bytes(mCurrentPosition)).intValue();

                String sformat = "";
                sformat = new String(read4Bytes((mCurrentPosition + 4)));

                if (sformat.equals("vide")) {
                    mFormat = TrakFormat.m4v;
                }
                if (sformat.equals("soun")) {
                    mFormat = TrakFormat.m4a;
                }
            }

            private int calculateBaseReference(int chunkOffset, int sampleNumber,
                                               int sampleInChunk, Stsz stsz) {

                int finalReference = chunkOffset;
                for (int i = 0; i < sampleInChunk ; i ++) {
                    finalReference += stsz.getmSamplesSize(i + sampleNumber);
                }

                return finalReference;

            }

            private byte[] calculateFrame(int startingReference, int size) {
                byte[] frame = new byte[size];
                for (int i = 0; i < size; i ++) {
                    frame[i] = mAllBytes[startingReference + i];
                }

                return frame;

            }

            private byte[] extractTrakData(Stco stco, Stsz stsz, Stsc stsc, int id) {

                // will be used in calculating current chunk number
                int chunkNumber = 0;
                int sampleNumber = 0;
                ByteArrayOutputStream frames = new ByteArrayOutputStream();

                // getting video stream
                if (id == 0) {
                    int frameStart = 0;

                    for (int i = 0; i < stco.getEntriesCount(); i++) {
                        byte[] frame = new byte[stsz.getmSamplesSize(i)];
                        frameStart = stco.getChunkOffset(i);
                        try {
                            for (int j = 0; j < stsz.getmSamplesSize(i); j++) {
                                frame[j] = mAllBytes[frameStart + j];
                            }
                            frames.write(frame);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    return frames.toByteArray();

                    // getting audio stream
                } else if (id == 1){
                    for (int i = 0; i < stsc.getmEntriesCount() - 1; i ++) {
                        int startingChunk = stsc.getmFirstChunk(i);
                        int endingChunk = stsc.getmFirstChunk(i + 1);
                        byte[] frame;

                        for (int chunk = 0; chunk < (endingChunk - startingChunk); chunk ++) {

                            for (int sampleInChunk = 0; sampleInChunk < stsc.getmSamplesPerChunk(i); sampleInChunk++) {
                                int baseReference = calculateBaseReference(stco.getChunkOffset(chunkNumber),
                                        sampleNumber,  sampleInChunk, stsz);
                                frame = calculateFrame(baseReference,
                                        stsz.getmSamplesSize(sampleInChunk + sampleNumber));
                                try {
                                    frames.write(frame);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            chunkNumber++;
                            sampleNumber += stsc.getmSamplesPerChunk(i);
                        }

                        if (i == stsc.getmEntriesCount() -2) {
                            int finalChunk = stsc.getmFirstChunk(i + 1);
                            for (int sampleInChunk = 0; sampleInChunk < stsc.getmSamplesPerChunk(i); sampleInChunk++) {
                                int baseReference = calculateBaseReference(stco.getChunkOffset(chunkNumber),
                                        sampleNumber,  sampleInChunk, stsz);
                                frame = calculateFrame(baseReference,
                                        stsz.getmSamplesSize(sampleInChunk + sampleNumber));
                                try {
                                    frames.write(frame);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                            }
                            chunkNumber++;
                            sampleNumber += stsc.getmSamplesPerChunk(i + 1);
                            FileOutputStream fout = null;
                            return frames.toByteArray();
                        }
                    }
                }
                return null;
            }

        }
    }


}

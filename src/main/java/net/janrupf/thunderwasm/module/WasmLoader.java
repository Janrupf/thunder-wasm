package net.janrupf.thunderwasm.module;

import net.janrupf.thunderwasm.data.Global;
import net.janrupf.thunderwasm.exports.*;
import net.janrupf.thunderwasm.imports.*;
import net.janrupf.thunderwasm.instructions.*;
import net.janrupf.thunderwasm.instructions.decoder.InstructionDecoder;
import net.janrupf.thunderwasm.instructions.reference.RefFunc;
import net.janrupf.thunderwasm.lookup.SectionLocator;
import net.janrupf.thunderwasm.module.encoding.*;
import net.janrupf.thunderwasm.module.section.*;
import net.janrupf.thunderwasm.module.section.segment.DataSegment;
import net.janrupf.thunderwasm.module.section.segment.DataSegmentMode;
import net.janrupf.thunderwasm.module.section.segment.ElementSegment;
import net.janrupf.thunderwasm.module.section.segment.ElementSegmentMode;
import net.janrupf.thunderwasm.data.Limits;
import net.janrupf.thunderwasm.types.*;
import net.janrupf.thunderwasm.util.ObjectUtil;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.util.*;

public class WasmLoader {
    private static final SectionLocator<?>[] SECTION_ORDER = {
            TypeSection.LOCATOR,
            ImportSection.LOCATOR,
            FunctionSection.LOCATOR,
            TableSection.LOCATOR,
            MemorySection.LOCATOR,
            GlobalSection.LOCATOR,
            ExportSection.LOCATOR,
            StartSection.LOCATOR,
            ElementSection.LOCATOR,
            DataCountSection.LOCATOR,
            CodeSection.LOCATOR,
            DataSection.LOCATOR
    };

    private final InputStream stream;
    private final InstructionRegistry instructionRegistry;
    private final CharsetDecoder utf8Decoder;
    private final boolean strictParsing;
    private final Set<Byte> seenSectionIds;
    private int importCounter;
    private Byte buffered;
    private long cursorPos;
    private int nextSectionIndex;

    public WasmLoader(InputStream stream, InstructionRegistry instructionRegistry) {
        this(stream, instructionRegistry, true);
    }

    public WasmLoader(InputStream stream, InstructionRegistry instructionRegistry, boolean strictParsing) {
        this.stream = contentStream(stream);
        this.instructionRegistry = instructionRegistry;
        this.strictParsing = strictParsing;

        this.utf8Decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        this.importCounter = 0;
        this.buffered = null;
        this.cursorPos = 0;
        this.nextSectionIndex = 0;
        this.seenSectionIds = new HashSet<>();
    }

    public WasmModule load() throws IOException, InvalidModuleException {
        byte[] header = this.requireBytes(4);
        if (header[0] != 0 || header[1] != 'a' || header[2] != 's' || header[3] != 'm') {
            throw new InvalidModuleException("Invalid magic header");
        }

        byte[] versionRaw = this.requireBytes(4);
        int version = (versionRaw[3] << 24) | (versionRaw[2] << 16) | (versionRaw[1] << 8) | versionRaw[0];
        if (version != 1) {
            throw new InvalidModuleException("Unsupported version: " + version);
        }

        List<WasmSection> sections = new ArrayList<>();

        // Read all the remaining data as sections
        while (!this.isEOF()) {
            WasmSection section = this.readSection();

            if (strictParsing && CustomSection.LOCATOR.getSectionId() != section.getId()) {
                if (this.nextSectionIndex >= SECTION_ORDER.length) {
                    throw new InvalidModuleException(
                            "Found section with id " + unsignedByteToString(section.getId()) +
                                    " after all standard sections");
                }

                boolean foundValid = false;
                for (int i = this.nextSectionIndex; i < SECTION_ORDER.length; i++) {
                    if (SECTION_ORDER[i].getSectionId() == section.getId()) {
                        this.nextSectionIndex = i + 1;
                        foundValid = true;
                        break;
                    }
                }

                if (!foundValid) {
                    throw new InvalidModuleException(
                            "Section with id " + unsignedByteToString(section.getId()) +
                                    " is out of order, expected section with id " +
                                    unsignedByteToString(SECTION_ORDER[this.nextSectionIndex].getSectionId()) +
                                    " or later");
                }
            }

            sections.add(section);
            seenSectionIds.add(section.getId());
        }

        if (strictParsing) {
            runBasicValidation(sections);
        }

        return new WasmModule(version, sections);
    }

    /**
     * Determine whether a section with the given locator has been seen while loading the module.
     *
     * @param locator the locator of the section to check
     * @return true if a section with the given locator has been seen, false otherwise
     */
    public boolean hasSeenSection(SectionLocator<?> locator) {
        return seenSectionIds.contains(locator.getSectionId());
    }

    /**
     * Run some very basic validation on the module sections.
     * <p>
     * This is mainly done because the WASM spec mandates it, though some of these checks
     * could also be interpreted as part of the actual validation step.
     *
     * @param sections the sections to validate
     * @throws InvalidModuleException if the module is invalid
     */
    private void runBasicValidation(List<WasmSection> sections) throws InvalidModuleException {
        long functionSectionLength = 0;
        long codeSectionLength = 0;
        long dataCountSectionLength = -1;
        long dataSectionLength = 0;

        for (WasmSection section : sections) {
            if (section instanceof FunctionSection) {
                functionSectionLength = ((FunctionSection) section).getTypes().length();
            } else if (section instanceof CodeSection) {
                codeSectionLength = ((CodeSection) section).getFunctions().length();
            } else if (section instanceof DataCountSection) {
                dataCountSectionLength = ((DataCountSection) section).getCount();
            } else if (section instanceof DataSection) {
                dataSectionLength = ((DataSection) section).getSegments().length();
            }
        }

        if (functionSectionLength != codeSectionLength) {
            throw new InvalidModuleException(
                    "Function and code section lengths do not match, function section length: " + functionSectionLength
                            + ", code section length: " + codeSectionLength);
        }

        if (dataCountSectionLength != -1 && dataCountSectionLength != dataSectionLength) {
            throw new InvalidModuleException(
                    "Data count and data section lengths do not match, data count section length: " +
                            dataCountSectionLength + ", data section length: " + dataSectionLength);
        }
    }

    private WasmSection readSection() throws IOException, InvalidModuleException {
        byte id = this.requireByte();
        int sectionSize = this.readU32();
        long sectionStartPos = this.cursorPos;

        WasmSection section;

        switch (id) {
            case 0: {
                String name = this.readName();

                long remainingSectionBytes = sectionStartPos + sectionSize - this.cursorPos;
                LargeByteArray data = this.readByteVecBody(LargeArrayIndex.fromU64(remainingSectionBytes));

                section = new CustomSection(id, name, data);
                break;
            }

            case 1: {
                section = this.readTypeSection(id);
                break;
            }

            case 2: {
                section = this.readImportSection(id);
                break;
            }

            case 3: {
                section = this.readFunctionSection(id);
                break;
            }

            case 4: {
                section = this.readTableSection(id);
                break;
            }

            case 5: {
                section = this.readMemorySection(id);
                break;
            }

            case 6: {
                section = this.readGlobalSection(id);
                break;
            }

            case 7: {
                section = this.readExportSection(id);
                break;
            }

            case 8: {
                section = this.readStartSection(id);
                break;
            }

            case 9: {
                section = this.readElementSection(id);
                break;
            }

            case 10: {
                section = this.readCodeSection(id);
                break;
            }

            case 11: {
                section = this.readDataSection(id);
                break;
            }

            case 12: {
                section = this.readDataCountSection(id);
                break;
            }

            default: {
                if (strictParsing) {
                    throw new InvalidModuleException("Invalid section id: " + unsignedByteToString(id));
                }

                LargeByteArray data = this.readByteVecBody(LargeArrayIndex.fromU32(sectionSize));
                section = new UnidentifiedSection(id, data);
                break;
            }
        }

        // Check if section size matches what we have read
        long sectionEndPos = this.cursorPos;
        if (sectionEndPos - sectionStartPos != sectionSize) {
            throw new InvalidModuleException("Section " + id + " size does not match the size that was actually read, expected " + sectionSize + " bytes, but read " + (sectionEndPos - sectionStartPos) + " bytes");
        }

        return section;
    }

    /**
     * Read a type section from the stream.
     *
     * @param id the section id
     * @return the read type section
     * @throws IOException            if an I/O error occurs
     * @throws InvalidModuleException if the module is invalid
     */
    public TypeSection readTypeSection(byte id) throws IOException, InvalidModuleException {
        LargeArray<FunctionType> types = this.readVec(FunctionType.class, this::readFunctionType);
        return new TypeSection(id, types);
    }

    /**
     * Read an import section from the stream.
     *
     * @param id the section id
     * @return the read import section
     * @throws IOException            if an I/O error occurs
     * @throws InvalidModuleException if the module is invalid
     */
    public ImportSection readImportSection(byte id) throws IOException, InvalidModuleException {
        LargeArray<Import<?>> imports = ObjectUtil.forceCast(this.readVec(Import.class, this::readImport));
        return new ImportSection(id, imports);
    }

    /**
     * Read a function section from the stream.
     *
     * @param id the section id
     * @return the read function section
     * @throws IOException            if an I/O error occurs
     * @throws InvalidModuleException if the module is invalid
     */
    public FunctionSection readFunctionSection(byte id) throws IOException, InvalidModuleException {
        LargeIntArray types = this.readU32Vec();
        return new FunctionSection(id, types);
    }

    /**
     * Read a table section from the stream.
     *
     * @param id the section id
     * @return the read table section
     * @throws IOException            if an I/O error occurs
     * @throws InvalidModuleException if the module is invalid
     */
    public TableSection readTableSection(byte id) throws IOException, InvalidModuleException {
        LargeArray<TableType> types = this.readVec(TableType.class, this::readTableType);
        return new TableSection(id, types);
    }

    /**
     * Read a memory section from the stream.
     *
     * @param id the section id
     * @return the read memory section
     * @throws IOException            if an I/O error occurs
     * @throws InvalidModuleException if the module is invalid
     */
    public MemorySection readMemorySection(byte id) throws IOException, InvalidModuleException {
        LargeArray<MemoryType> types = this.readVec(MemoryType.class, this::readMemoryType);
        return new MemorySection(id, types);
    }

    /**
     * Read a global section from the stream.
     *
     * @param id the section id
     * @return the read global section
     * @throws IOException            if an I/O error occurs
     * @throws InvalidModuleException if the module is invalid
     */
    public GlobalSection readGlobalSection(byte id) throws IOException, InvalidModuleException {
        LargeArray<Global> globals = this.readVec(Global.class, this::readGlobal);
        return new GlobalSection(id, globals);
    }

    /**
     * Read an export section from the stream.
     *
     * @param id the section id
     * @return the read export section
     * @throws IOException            if an I/O error occurs
     * @throws InvalidModuleException if the module is invalid
     */
    public ExportSection readExportSection(byte id) throws IOException, InvalidModuleException {
        LargeArray<Export<?>> exports = ObjectUtil.forceCast(this.readVec(Export.class, this::readExport));
        return new ExportSection(id, exports);
    }

    /**
     * Read a start section from the stream.
     *
     * @param id the section id
     * @return the read start section
     * @throws IOException if an I/O error occurs
     */
    public StartSection readStartSection(byte id) throws IOException {
        int index = this.readU32();
        return new StartSection(id, index);
    }

    /**
     * Read an element section from the stream.
     *
     * @param id the section id
     * @return the read element section
     * @throws IOException            if an I/O error occurs
     * @throws InvalidModuleException if the module is invalid
     */
    public ElementSection readElementSection(byte id) throws IOException, InvalidModuleException {
        LargeArray<ElementSegment> segments = this.readVec(ElementSegment.class, this::readElementSegment);
        return new ElementSection(id, segments);
    }

    /**
     * Read a code section from the stream.
     *
     * @param id the section id
     * @return the read code section
     * @throws IOException            if an I/O error occurs
     * @throws InvalidModuleException if the module is invalid
     */
    public CodeSection readCodeSection(byte id) throws IOException, InvalidModuleException {
        LargeArray<Function> functions = this.readVec(Function.class, this::readFunction);
        return new CodeSection(id, functions);
    }

    /**
     * Read a data section from the stream.
     *
     * @param id the section id
     * @return the read data section
     * @throws IOException            if an I/O error occurs
     * @throws InvalidModuleException if the module is invalid
     */
    public DataSection readDataSection(byte id) throws IOException, InvalidModuleException {
        LargeArray<DataSegment> segments = this.readVec(DataSegment.class, this::readDataSegment);
        return new DataSection(id, segments);
    }

    /**
     * Read a data count section from the stream.
     *
     * @param id the section id
     * @return the read data count section
     * @throws IOException if an I/O error occurs
     */
    public DataCountSection readDataCountSection(byte id) throws IOException {
        int count = this.readU32();
        return new DataCountSection(id, count);
    }

    /**
     * Read an element segment from the stream.
     *
     * @return the read element segment
     * @throws IOException            if an I/O error occurs
     * @throws InvalidModuleException if the module is invalid
     */
    public ElementSegment readElementSegment() throws IOException, InvalidModuleException {
        int type = this.readU32();

        VecElementReader<Expr> functionIndexAsExpr = () -> {
            if (!instructionRegistry.has(RefFunc.INSTANCE)) {
                throw new InvalidModuleException("The available instruction set does not contain the necessary instruction to" + " read function indices as expressions");
            }

            // This is kind of hacky, but works -
            // just interpret the function index as the data of a RefFunc instruction
            InstructionInstance instruction = this.readInstructionData(RefFunc.INSTANCE);
            return new Expr(Collections.singletonList(instruction));
        };

        ReferenceType referenceType;
        LargeArray<Expr> init;
        ElementSegmentMode mode;

        // For some reason element segments are bit messy in the WASM spec
        switch (type) {
            case 0: {
                Expr offsetExpr = Expr.read(this);

                referenceType = ReferenceType.FUNCREF;
                init = this.readVec(Expr.class, functionIndexAsExpr);
                mode = new ElementSegmentMode.Active(0, offsetExpr);
                break;
            }

            case 1: {
                this.expectByte((byte) 0x00);

                referenceType = ReferenceType.FUNCREF;
                init = this.readVec(Expr.class, functionIndexAsExpr);
                mode = ElementSegmentMode.Passive.INSTANCE;
                break;
            }

            case 2: {
                int tableIndex = this.readU32();
                Expr offsetExpr = Expr.read(this);

                this.expectByte((byte) 0x00);

                referenceType = ReferenceType.FUNCREF;
                init = this.readVec(Expr.class, functionIndexAsExpr);
                mode = new ElementSegmentMode.Active(tableIndex, offsetExpr);
                break;
            }

            case 3: {
                this.expectByte((byte) 0x00);

                referenceType = ReferenceType.FUNCREF;
                init = this.readVec(Expr.class, functionIndexAsExpr);
                mode = ElementSegmentMode.Declarative.INSTANCE;
                break;
            }

            case 4: {
                Expr offsetExpr = Expr.read(this);

                referenceType = ReferenceType.FUNCREF;
                init = this.readVec(Expr.class, () -> Expr.read(this));
                mode = new ElementSegmentMode.Active(0, offsetExpr);
                break;
            }

            case 5: {
                referenceType = this.readReferenceType();
                init = this.readVec(Expr.class, () -> Expr.read(this));
                mode = ElementSegmentMode.Passive.INSTANCE;
                break;
            }

            case 6: {
                int tableIndex = this.readU32();
                Expr offsetExpr = Expr.read(this);

                referenceType = this.readReferenceType();
                init = this.readVec(Expr.class, () -> Expr.read(this));
                mode = new ElementSegmentMode.Active(tableIndex, offsetExpr);
                break;
            }

            case 7: {
                referenceType = this.readReferenceType();
                init = this.readVec(Expr.class, () -> Expr.read(this));
                mode = ElementSegmentMode.Declarative.INSTANCE;
                break;
            }

            default:
                throw new InvalidModuleException("Invalid element segment type: " + type);
        }

        return new ElementSegment(referenceType, mode, init);
    }

    /**
     * Read a data segment from the stream.
     *
     * @return the read data segment
     * @throws IOException            if an I/O error occurs
     * @throws InvalidModuleException if the module is invalid
     */
    public DataSegment readDataSegment() throws IOException, InvalidModuleException {
        int type = this.readU32();

        LargeByteArray init;
        DataSegmentMode mode;

        switch (type) {
            case 0: {
                Expr offsetExpr = Expr.read(this);

                init = this.readByteVec();
                mode = new DataSegmentMode.Active(0, offsetExpr);
                break;
            }

            case 1: {
                init = this.readByteVec();
                mode = DataSegmentMode.Passive.INSTANCE;
                break;
            }

            case 2: {
                int memoryIndex = this.readU32();
                Expr offsetExpr = Expr.read(this);

                init = this.readByteVec();
                mode = new DataSegmentMode.Active(memoryIndex, offsetExpr);
                break;
            }

            default:
                throw new InvalidModuleException("Invalid data segment type: " + type);
        }

        return new DataSegment(init, mode);
    }

    /**
     * Read a function from the stream.
     *
     * @return the read function
     * @throws IOException            if an I/O error occurs
     * @throws InvalidModuleException if the module is invalid
     */
    public Function readFunction() throws IOException, InvalidModuleException {
        int size = this.readU32();
        long currentCursorPos = this.cursorPos;

        LargeArray<Local> locals = this.readVec(Local.class, this::readLocal);

        if (strictParsing) {
            long localCount = 0;

            for (Local l : locals) {
                localCount += Integer.toUnsignedLong(l.getCount());

                if (Long.compareUnsigned(localCount, 0xFFFFFFFFL) > 0) {
                    throw new InvalidModuleException("Function has too many locals (" + localCount + ")");
                }
            }
        }

        Expr expr = Expr.read(this);

        if (this.cursorPos - currentCursorPos != size) {
            throw new InvalidModuleException("Function size does not match the size that was actually read, expected " + size + " bytes, but read" + " " + (this.cursorPos - currentCursorPos) + " bytes");
        }

        return new Function(expr, locals);
    }

    /**
     * Read a local from the stream.
     *
     * @return the read local
     * @throws IOException            if an I/O error occurs
     * @throws InvalidModuleException if the module is invalid
     */
    public Local readLocal() throws IOException, InvalidModuleException {
        int count = this.readU32();
        ValueType type = this.readValueType();
        return new Local(count, type);
    }

    /**
     * Read an import from the stream.
     *
     * @return the read import
     * @throws IOException            if an I/O error occurs
     * @throws InvalidModuleException if the module is invalid
     */
    public Import<?> readImport() throws IOException, InvalidModuleException {
        String module = this.readName();
        String name = this.readName();

        byte kind = this.requireByte();
        ImportDescription description;

        switch (kind) {
            case 0x00: {
                int typeIdx = this.readU32();
                description = new TypeImportDescription(typeIdx);
                break;
            }

            case 0x01: {
                TableType tt = this.readTableType();
                description = new TableImportDescription(tt);
                break;
            }

            case 0x02: {
                MemoryType mt = this.readMemoryType();
                description = new MemoryImportDescription(mt);
                break;
            }

            case 0x03: {
                GlobalType gt = this.readGlobalType();
                description = new GlobalImportDescription(gt);
                break;
            }

            default: {
                throw new InvalidModuleException("Invalid import kind: " + unsignedByteToString(kind));
            }
        }

        return new Import<>(module, name, description, importCounter++);
    }

    /**
     * Read an export from the stream.
     *
     * @return the read export
     * @throws IOException            if an I/O error occurs
     * @throws InvalidModuleException if the module is invalid
     */
    public Export readExport() throws IOException, InvalidModuleException {
        String name = this.readName();
        byte kind = this.requireByte();
        int index = this.readU32();

        ExportDescription description;

        switch (kind) {
            case 0x00: {
                description = new FunctionExportDescription(index);
                break;
            }

            case 0x01: {
                description = new TableExportDescription(index);
                break;
            }

            case 0x02: {
                description = new MemoryExportDescription(index);
                break;
            }

            case 0x03: {
                description = new GlobalExportDescription(index);
                break;
            }

            default: {
                throw new InvalidModuleException("Invalid export kind: " + unsignedByteToString(kind));
            }
        }

        return new Export(name, description);
    }

    /**
     * Reads a function type from the stream.
     *
     * @return the read function type
     * @throws IOException            if an I/O error occurs
     * @throws InvalidModuleException if the module is invalid
     */
    public FunctionType readFunctionType() throws IOException, InvalidModuleException {
        this.expectByte((byte) 0x60);
        return this.readFunctionTypeBody();
    }

    /**
     * Reads the body of a function type from the stream.
     *
     * @return the read function type
     * @throws IOException            if an I/O error occurs
     * @throws InvalidModuleException if the module is invalid
     */
    public FunctionType readFunctionTypeBody() throws IOException, InvalidModuleException {
        LargeArray<ValueType> inputs = this.readVec(ValueType.class, this::readValueType);
        LargeArray<ValueType> outputs = this.readVec(ValueType.class, this::readValueType);

        return new FunctionType(inputs, outputs);
    }

    /**
     * Read a value type from the stream.
     *
     * @return the read value type
     * @throws IOException            if an I/O error occurs
     * @throws InvalidModuleException if the module is invalid
     */
    public ValueType readValueType() throws IOException, InvalidModuleException {
        byte valueType = this.requireByte();
        return constructValueType(valueType);
    }

    /**
     * Construct the value type from the raw byte.
     *
     * @param valueType the value type byte
     * @return the real value type
     * @throws InvalidModuleException if the value type code is invalid
     */
    public ValueType constructValueType(byte valueType) throws InvalidModuleException {
        switch (valueType) {
            case 0x7F:
                return NumberType.I32;
            case 0x7E:
                return NumberType.I64;
            case 0x7D:
                return NumberType.F32;
            case 0x7C:
                return NumberType.F64;
            case 0x7B:
                return VecType.V128;
            case 0x70:
                return ReferenceType.FUNCREF;
            case 0x6F:
                return ReferenceType.EXTERNREF;
            default:
                throw new InvalidModuleException("Invalid value type: " + unsignedByteToString(valueType));
        }
    }

    /**
     * Read a reference type from the stream.
     *
     * @return the read reference type
     * @throws IOException            if an I/O error occurs
     * @throws InvalidModuleException if the module is invalid
     */
    public ReferenceType readReferenceType() throws IOException, InvalidModuleException {
        ValueType valueType = this.readValueType();

        if (!(valueType instanceof ReferenceType)) {
            throw new InvalidModuleException("Expected reference type, but got " + valueType);
        }

        return (ReferenceType) valueType;
    }

    /**
     * Read a table type from the stream.
     *
     * @return the read table type
     * @throws IOException            if an I/O error occurs
     * @throws InvalidModuleException if the module is invalid
     */
    public TableType readTableType() throws IOException, InvalidModuleException {
        ReferenceType refType = this.readReferenceType();
        Limits limits = this.readLimits();

        return new TableType(refType, limits);
    }

    /**
     * Read limits from the stream.
     *
     * @return the read limits
     * @throws IOException            if an I/O error occurs
     * @throws InvalidModuleException if the module is invalid
     */
    public Limits readLimits() throws IOException, InvalidModuleException {
        byte kind = this.requireByte();

        switch (kind) {
            case 0x00: {
                int min = this.readU32();
                return new Limits(min, null);
            }

            case 0x01: {
                int min = this.readU32();
                int max = this.readU32();
                return new Limits(min, max);
            }

            default: {
                throw new InvalidModuleException("Invalid limits kind: " + unsignedByteToString(kind));
            }
        }
    }

    /**
     * Read a memory type from the stream.
     *
     * @return the read memory type
     * @throws IOException            if an I/O error occurs
     * @throws InvalidModuleException if the module is invalid
     */
    public MemoryType readMemoryType() throws IOException, InvalidModuleException {
        Limits limits = this.readLimits();
        return new MemoryType(limits);
    }

    /**
     * Read a global from the stream.
     *
     * @return the read global
     * @throws IOException            if an I/O error occurs
     * @throws InvalidModuleException if the module is invalid
     */
    public Global readGlobal() throws IOException, InvalidModuleException {
        GlobalType type = this.readGlobalType();
        Expr init = Expr.read(this);
        return new Global(type, init);
    }

    /**
     * Read a global type from the stream.
     *
     * @return the read global type
     * @throws IOException            if an I/O error occurs
     * @throws InvalidModuleException if the module is invalid
     */
    public GlobalType readGlobalType() throws IOException, InvalidModuleException {
        ValueType vt = this.readValueType();
        byte rawMutability = this.requireByte();

        GlobalType.Mutability mutability;
        switch (rawMutability) {
            case 0x00: {
                mutability = GlobalType.Mutability.CONST;
                break;
            }

            case 0x01: {
                mutability = GlobalType.Mutability.VAR;
                break;
            }

            default: {
                throw new InvalidModuleException("Invalid global type mutability: " + unsignedByteToString(rawMutability));
            }
        }

        return new GlobalType(mutability, vt);
    }

    /**
     * Read and require n bytes from the stream
     *
     * @param out the array to fill
     * @return the array passed in
     * @throws IOException if the stream is closed or the end of the stream is reached
     */
    public byte[] requireBytes(byte[] out) throws IOException {
        int read = stream.read(out);

        if (read != out.length) {
            throw new EOFException("Expected " + out.length + " bytes, but got " + read + " bytes");
        }

        return out;
    }

    /**
     * Read and require n bytes from the stream.
     * <p>
     * This function is a convenience wrapper around {@link #requireBytes(byte[])} that
     * creates a new byte array of size n.
     *
     * @param n the number of bytes to read
     * @return the read bytes
     * @throws IOException if an I/O error occurs
     */
    public byte[] requireBytes(int n) throws IOException {
        return requireBytes(new byte[n]);
    }

    /**
     * Read and require 1 byte from the stream.
     *
     * @return the read byte
     * @throws IOException if an I/O error occurs
     */
    public byte requireByte() throws IOException {
        int val = stream.read();
        if (val == -1) {
            throw new EOFException("Expected 1 byte, but got 0 bytes");
        } else {
            return (byte) val;
        }
    }

    /**
     * Read and require a specific byte from the stream.
     *
     * @param value the byte to expect
     * @throws IOException if an I/O error occurs
     */
    public void expectByte(byte value) throws IOException {
        byte read = requireByte();
        if (read != value) {
            throw new IOException("Expected byte " + unsignedByteToString(value) + ", but got " + unsignedByteToString(read));
        }
    }

    /**
     * Determine if the stream is at the end of the file.
     *
     * @return true if the stream is at the end of the file, false otherwise
     * @throws IOException if an I/O error occurs
     */
    public boolean isEOF() throws IOException {
        if (this.buffered != null) {
            return false;
        }

        int val = stream.read();
        this.cursorPos--; // We have to decrement the cursor position because we have not actually read a byte
        if (val == -1) {
            return true;
        } else {
            this.buffered = (byte) val;
            return false;
        }
    }

    /**
     * Peek at the next byte in the stream without consuming it.
     *
     * @return the next byte in the stream
     * @throws IOException if an I/O error occurs
     */
    public byte peekByte() throws IOException {
        if (this.buffered != null) {
            return this.buffered;
        }

        int val = stream.read();
        this.cursorPos--; // We have to decrement the cursor position because we have not actually read a byte

        if (val == -1) {
            throw new EOFException("Expected 1 byte, but got 0 bytes");
        } else {
            this.buffered = (byte) val;
            return this.buffered;
        }
    }

    /**
     * Read a vector of bytes from the stream.
     *
     * @return the read byte vector
     * @throws IOException if an I/O error occurs
     */
    public LargeByteArray readByteVec() throws IOException {
        LargeArrayIndex size = LargeArrayIndex.fromU32(this.readU32());
        return this.readByteVecBody(size);
    }

    /**
     * Read the body of a byte vector from the stream.
     *
     * @param size the size of the byte vector
     * @return the read byte vector
     * @throws IOException if an I/O error occurs
     */
    public LargeByteArray readByteVecBody(LargeArrayIndex size) throws IOException {
        LargeByteArray array = new LargeByteArray(size);

        LargeArrayIndex i = LargeArrayIndex.ZERO;
        while (i.compareTo(size) < 0) {
            long remaining = size.subtract(i).toU64();

            int readSize;
            if (Long.compareUnsigned(remaining, Integer.MAX_VALUE) > 0) {
                readSize = Integer.MAX_VALUE;
            } else {
                readSize = (int) remaining;
            }

            byte[] buffer = this.requireBytes(readSize);
            array.setRegion(i, buffer);

            i = i.add(LargeArrayIndex.fromU32(buffer.length));
        }

        return array;
    }

    /**
     * Read a vector of I32's from the stream.
     *
     * @return the read I32 vector
     * @throws IOException            if an I/O error occurs
     * @throws InvalidModuleException if the module is invalid
     */
    public LargeIntArray readS32Vec() throws IOException, InvalidModuleException {
        LargeArrayIndex size = LargeArrayIndex.fromU32(this.readU32());
        return this.readS32VecBody(size);
    }

    /**
     * Read the body of an I32 vector from the stream.
     *
     * @param size the size of the I32 vector
     * @return the read I32 vector
     * @throws IOException            if an I/O error occurs
     * @throws InvalidModuleException if the module is invalid
     */
    public LargeIntArray readS32VecBody(LargeArrayIndex size) throws IOException, InvalidModuleException {
        return this.readIntVecBody(size, this::readS32);
    }

    /**
     * Read a vector of U32's from the stream.
     *
     * @return the read U32 vector
     * @throws IOException            if an I/O error occurs
     * @throws InvalidModuleException if the module is invalid
     */
    public LargeIntArray readU32Vec() throws IOException, InvalidModuleException {
        LargeArrayIndex size = LargeArrayIndex.fromU32(this.readU32());
        return this.readU32VecBody(size);
    }

    /**
     * Read the body of a U32 vector from the stream.
     *
     * @param size the size of the U32 vector
     * @return the read U32 vector
     * @throws IOException            if an I/O error occurs
     * @throws InvalidModuleException if the module is invalid
     */
    public LargeIntArray readU32VecBody(LargeArrayIndex size) throws IOException, InvalidModuleException {
        return this.readIntVecBody(size, this::readU32);
    }

    /**
     * Read the body of an int vector from the stream.
     *
     * @param size the size of the int vector
     * @return the read int vector
     * @throws IOException if an I/O error occurs
     */
    public LargeIntArray readIntVecBody(LargeArrayIndex size, Int32VecElementReader reader) throws IOException, InvalidModuleException {
        LargeIntArray array = new LargeIntArray(size);

        for (LargeArrayIndex i = LargeArrayIndex.ZERO; i.compareTo(size) < 0; i = i.add(1)) {
            int element = reader.readNext();
            array.set(i, element);
        }

        return array;
    }

    /**
     * Read a vector of elements from the stream.
     *
     * @param reader the reader to read elements with
     * @param <T>    the type of the elements
     * @return the read vector
     * @throws IOException            if an I/O error occurs
     * @throws InvalidModuleException if the module is invalid
     */
    public <T> LargeArray<T> readVec(Class<T> valueType, VecElementReader<T> reader) throws IOException, InvalidModuleException {
        LargeArrayIndex size = LargeArrayIndex.fromU32(this.readU32());
        return this.readVecBody(valueType, size, reader);
    }

    /**
     * Read the body of a vector of elements from the stream.
     *
     * @param valueType the type of the elements
     * @param size      the size of the vector
     * @param reader    the reader to read elements with
     * @param <T>       the type of the elements
     * @return the read vector
     * @throws IOException            if an I/O error occurs
     * @throws InvalidModuleException if the module is invalid
     */
    public <T> LargeArray<T> readVecBody(Class<T> valueType, LargeArrayIndex size, VecElementReader<T> reader) throws IOException, InvalidModuleException {
        LargeArray<T> array = new LargeArray<>(valueType, size);
        for (LargeArrayIndex i = LargeArrayIndex.ZERO; i.compareTo(size) < 0; i = i.add(1)) {
            T element = reader.readNext();
            array.set(i, element);
        }

        return array;
    }

    /**
     * Read a name from the stream.
     *
     * @return the read name
     * @throws IOException if an I/O error occurs
     */
    public String readName() throws IOException, InvalidModuleException {
        LargeByteArray array = this.readByteVec();
        byte[] raw = array.asFlatArray();

        if (raw == null) {
            throw new IOException("Name is too large");
        }

        try {
            return this.utf8Decoder.decode(ByteBuffer.wrap(raw)).toString();
        } catch (CharacterCodingException e) {
            throw new InvalidModuleException("Invalid name", e);
        }
    }

    /**
     * Read an unsigned 16-bit LEB128 value from the stream.
     *
     * @return the read value
     * @throws IOException if an I/O error occurs
     */
    public short readU16() throws IOException {
        return LEB128Value.readU16(stream);
    }

    /**
     * Read an unsigned 32-bit LEB128 value from the stream.
     *
     * @return the read value
     * @throws IOException if an I/O error occurs
     */
    public int readU32() throws IOException {
        return LEB128Value.readU32(stream);
    }

    /**
     * Read an unsigned 64-bit LEB128 value from the stream.
     *
     * @return the read value
     * @throws IOException if an I/O error occurs
     */
    public long readU64() throws IOException {
        return LEB128Value.readU64(stream);
    }

    /**
     * Read a signed 16-bit LEB128 value from the stream.
     *
     * @return the read value
     * @throws IOException if an I/O error occurs
     */
    public short readS16() throws IOException {
        return LEB128Value.readS16(stream);
    }

    /**
     * Read a signed 32-bit LEB128 value from the stream.
     *
     * @return the read value
     * @throws IOException if an I/O error occurs
     */
    public int readS32() throws IOException {
        return LEB128Value.readS32(stream);
    }

    /**
     * Read a signed 64-bit LEB128 value from the stream.
     *
     * @return the read value
     * @throws IOException if an I/O error occurs
     */
    public long readS64() throws IOException {
        return LEB128Value.readS64(stream);
    }

    /**
     * Read a 32-bit floating point value from the stream.
     *
     * @return the read value
     * @throws IOException if an I/O error occurs
     */
    public float readF32() throws IOException {
        byte[] raw = this.requireBytes(4);
        int bits = ((((int) raw[3]) & 0xFF) << 24) | ((((int) raw[2]) & 0xFF) << 16) | ((((int) raw[1]) & 0xFF) << 8) | (((int) raw[0]) & 0xFF);
        return Float.intBitsToFloat(bits);
    }

    /**
     * Read a 64-bit floating point value from the stream.
     *
     * @return the read value
     * @throws IOException if an I/O error occurs
     */
    public double readF64() throws IOException {
        byte[] raw = this.requireBytes(8);
        long bits = ((((long) raw[7]) & 0xFF) << 56) | ((((long) raw[6]) & 0xFF) << 48) | ((((long) raw[5]) & 0xFF) << 40) | ((((long) raw[4]) & 0xFF) << 32) | ((((long) raw[3]) & 0xFF) << 24) | ((((long) raw[2]) & 0xFF) << 16) | ((((long) raw[1]) & 0xFF) << 8) | ((((long) raw[0]) & 0xFF));
        return Double.longBitsToDouble(bits);
    }

    /**
     * Read an instruction from the stream.
     *
     * @return the read instruction
     * @throws IOException            if an I/O error occurs
     * @throws InvalidModuleException if the module is invalid
     */
    public InstructionInstance readInstruction() throws IOException, InvalidModuleException {
        byte opCode = this.requireByte();
        return this.readInstructionData(opCode);
    }

    /**
     * Read the data of an instruction from the stream.
     *
     * @param opCode the opcode of the instruction
     * @return the read instruction
     * @throws IOException            if an I/O error occurs
     * @throws InvalidModuleException if the module is invalid
     */
    public InstructionInstance readInstructionData(byte opCode) throws IOException, InvalidModuleException {
        InstructionDecoder decoder = instructionRegistry.getDecoder(opCode);
        WasmInstruction<?> instruction = decoder.decode(opCode, this);

        return this.readInstructionData(instruction);
    }

    /**
     * Read a block type from the stream.
     *
     * @return the read block type
     * @throws IOException            if an I/O error occurs
     * @throws InvalidModuleException if the module is invalid
     */
    public BlockType readBlockType() throws IOException, InvalidModuleException {
        long code = this.readS64();

        if (code >= 0) {
            return new BlockType.TypeIndex((int) code);
        } else if (code == -64 /* 0x40 */) {
            return BlockType.Empty.INSTANCE;
        } else {
            ValueType type = constructValueType((byte) (code & 0x7F));
            return new BlockType.Value(type);
        }
    }

    /**
     * Read the data of an instruction from the stream.
     *
     * @param instruction the instruction to read the data for
     * @param <T>         the type of the instruction data
     * @return the read instruction
     * @throws IOException            if an I/O error occurs
     * @throws InvalidModuleException if the module is invalid
     */
    public <T extends WasmInstruction.Data> InstructionInstance readInstructionData(WasmInstruction<T> instruction) throws IOException, InvalidModuleException {
        T data = instruction.readData(this);
        return new InstructionInstance(instruction, data);
    }

    /**
     * Checks if strict parsing is enabled.
     *
     * @return true if strict parsing is enabled, false otherwise
     */
    public boolean isStrictParsing() {
        return strictParsing;
    }

    private String unsignedByteToString(byte b) {
        return Integer.toString(Byte.toUnsignedInt(b));
    }

    /**
     * Retrieves an input stream which counts the cursor position
     * when reading from it.
     *
     * @return the input stream
     */
    private InputStream contentStream(InputStream stream) {
        return new InputStream() {
            @Override
            public int read() throws IOException {
                if (buffered != null) {
                    byte buffered = WasmLoader.this.buffered;
                    WasmLoader.this.buffered = null;
                    cursorPos++;
                    return Byte.toUnsignedInt(buffered);
                }

                int val = stream.read();
                if (val != -1) {
                    cursorPos++;
                }

                return val;
            }

            @Override
            public int read(byte[] b) throws IOException {
                int readCount;

                if (buffered != null) {
                    b[0] = buffered;
                    buffered = null;
                    readCount = stream.read(b, 1, b.length - 1) + 1;
                } else {
                    readCount = stream.read(b);
                }

                if (readCount != -1) {
                    cursorPos += readCount;
                }

                return readCount;
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                int readCount;

                if (buffered != null) {
                    b[off] = buffered;
                    buffered = null;
                    readCount = stream.read(b, off + 1, len - 1) + 1;
                } else {
                    readCount = stream.read(b, off, len);
                }

                if (readCount != -1) {
                    cursorPos += readCount;
                }

                return readCount;
            }

            @Override
            public long skip(long n) throws IOException {
                if (buffered != null) {
                    buffered = null;
                    cursorPos++;
                    n--;
                }

                long val = super.skip(n);
                cursorPos += val;
                return val;
            }

            @Override
            public int available() throws IOException {
                return super.available();
            }
        };
    }

    @FunctionalInterface
    public interface VecElementReader<T> {
        T readNext() throws IOException, InvalidModuleException;
    }

    @FunctionalInterface
    public interface Int32VecElementReader {
        int readNext() throws IOException, InvalidModuleException;
    }
}

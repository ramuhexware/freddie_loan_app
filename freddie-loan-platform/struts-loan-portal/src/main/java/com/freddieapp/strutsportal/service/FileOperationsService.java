package com.freddieapp.strutsportal.service;

import com.freddieapp.strutsportal.model.LoanDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * File-class-centric IO operations for the Struts Loan Portal.
 *
 * Every method in this service intentionally uses the classic {@code java.io.File}
 * API (not java.nio.file.Path) to demonstrate its capabilities in a real project.
 *
 * Operations covered:
 *  1.  createUploadDirectory()     — File.mkdirs(), File.exists(), File.isDirectory()
 *  2.  listLoanFiles()             — File.listFiles(), File.isFile(), File.length()
 *  3.  getFileMetadata()           — File.getName(), File.getParent(), File.canRead(),
 *                                    File.canWrite(), File.lastModified(), File.length()
 *  4.  moveFileToArchive()         — File.renameTo() + fallback copy-then-delete
 *  5.  renameDocument()            — File.renameTo()
 *  6.  deleteFileIfExists()        — File.exists(), File.delete()
 *  7.  purgeLoanDirectory()        — File.listFiles(), File.delete() (recursive)
 *  8.  writePropertiesSnapshot()   — FileOutputStream + Properties.store()
 *  9.  readPropertiesFromFile()    — FileInputStream + Properties.load()
 * 10.  writeIndexFile()            — FileWriter + BufferedWriter (append mode)
 * 11.  readIndexFile()             — FileReader + BufferedReader
 * 12.  copyFileClassic()           — FileInputStream → FileOutputStream (classic byte-copy)
 * 13.  filterFilesByExtension()    — FilenameFilter lambda
 * 14.  totalSizeOfDirectory()      — recursive File.listFiles() + File.length() sum
 * 15.  ensureDirectoryStructure()  — File.mkdirs() for the full portal dir tree
 */
@Slf4j
@Service
public class FileOperationsService {

    @Value("${portal.upload.dir:${java.io.tmpdir}/freddie-docs}")
    private String uploadBaseDir;

    @Value("${portal.reports.dir:${java.io.tmpdir}/freddie-reports}")
    private String reportsBaseDir;

    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ================================================================== //
    //  FILE OP #1 — createUploadDirectory                                 //
    //  Uses: File.mkdirs(), File.exists(), File.isDirectory()             //
    // ================================================================== //
    /**
     * Creates the upload directory for a specific loan if it doesn't exist.
     *
     * @return the created/existing {@link File} directory object
     */
    public File createUploadDirectory(String loanId) {
        File loanDir = new File(uploadBaseDir + File.separator + loanId);

        if (loanDir.exists() && loanDir.isDirectory()) {
            log.info("[File#1] Directory already exists: {}", loanDir.getAbsolutePath());
            return loanDir;
        }

        boolean created = loanDir.mkdirs();   // creates all parent directories too
        if (created) {
            log.info("[File#1] Created directory: {}", loanDir.getAbsolutePath());
        } else {
            log.warn("[File#1] Could not create directory: {}", loanDir.getAbsolutePath());
        }
        return loanDir;
    }

    // ================================================================== //
    //  FILE OP #2 — listLoanFiles                                         //
    //  Uses: File.listFiles(), File.isFile(), File.length(), File.getName() //
    // ================================================================== //
    /**
     * Lists all regular files in a loan's upload directory.
     *
     * @return list of Maps with name, absolutePath, size per file
     */
    public List<Map<String, Object>> listLoanFiles(String loanId) {
        File loanDir = new File(uploadBaseDir + File.separator + loanId);
        List<Map<String, Object>> result = new ArrayList<>();

        if (!loanDir.exists() || !loanDir.isDirectory()) {
            log.warn("[File#2] Directory does not exist: {}", loanDir.getAbsolutePath());
            return result;
        }

        File[] files = loanDir.listFiles();   // returns null if not a directory
        if (files == null) {
            log.warn("[File#2] listFiles() returned null for {}", loanDir.getAbsolutePath());
            return result;
        }

        for (File file : files) {
            if (file.isFile()) {              // skip subdirectories
                Map<String, Object> info = new LinkedHashMap<>();
                info.put("name",          file.getName());
                info.put("absolutePath",  file.getAbsolutePath());
                info.put("sizeBytes",     file.length());
                info.put("lastModified",  new Date(file.lastModified()).toString());
                info.put("canRead",       file.canRead());
                info.put("canWrite",      file.canWrite());
                result.add(info);
                log.debug("[File#2] Found: {} ({} bytes)", file.getName(), file.length());
            }
        }

        log.info("[File#2] Listed {} files in {}", result.size(), loanDir.getAbsolutePath());
        return result;
    }

    // ================================================================== //
    //  FILE OP #3 — getFileMetadata                                       //
    //  Uses: File.getName(), File.getParent(), File.getAbsolutePath(),    //
    //        File.length(), File.canRead(), File.canWrite(),              //
    //        File.canExecute(), File.lastModified(), File.isHidden()      //
    // ================================================================== //
    /**
     * Returns a rich metadata Map for a file using the java.io.File API.
     */
    public Map<String, Object> getFileMetadata(String absoluteFilePath) {
        File file = new File(absoluteFilePath);
        Map<String, Object> meta = new LinkedHashMap<>();

        meta.put("exists",        file.exists());
        meta.put("name",          file.getName());
        meta.put("parent",        file.getParent());
        meta.put("absolutePath",  file.getAbsolutePath());
        meta.put("canonicalPath", getCanonicalPathSafe(file));
        meta.put("sizeBytes",     file.length());
        meta.put("isFile",        file.isFile());
        meta.put("isDirectory",   file.isDirectory());
        meta.put("isHidden",      file.isHidden());
        meta.put("canRead",       file.canRead());
        meta.put("canWrite",      file.canWrite());
        meta.put("canExecute",    file.canExecute());
        meta.put("lastModified",  new Date(file.lastModified()).toString());

        log.info("[File#3] Metadata for '{}': exists={} size={} canRead={}",
                file.getName(), file.exists(), file.length(), file.canRead());
        return meta;
    }

    private String getCanonicalPathSafe(File file) {
        try { return file.getCanonicalPath(); }
        catch (IOException e) { return file.getAbsolutePath(); }
    }

    // ================================================================== //
    //  FILE OP #4 — moveFileToArchive                                     //
    //  Uses: File.renameTo() with fallback copy-and-delete                //
    // ================================================================== //
    /**
     * Moves a document file into an archive subdirectory.
     * Tries File.renameTo() first (atomic on same filesystem);
     * falls back to stream copy + delete if renameTo fails (cross-filesystem).
     *
     * @return the File object of the archived file
     */
    public File moveFileToArchive(String absoluteFilePath, String loanId) throws IOException {
        File sourceFile = new File(absoluteFilePath);
        if (!sourceFile.exists()) {
            throw new FileNotFoundException("Source file not found: " + absoluteFilePath);
        }

        File archiveDir = new File(uploadBaseDir + File.separator + loanId
                + File.separator + "archive");
        if (!archiveDir.exists()) {
            archiveDir.mkdirs();   // File.mkdirs() to create archive subdir
        }

        File destFile = new File(archiveDir, sourceFile.getName());

        // Attempt atomic rename (works when source and dest are on same filesystem)
        boolean renamed = sourceFile.renameTo(destFile);   // File.renameTo()
        if (renamed) {
            log.info("[File#4] Renamed/moved '{}' → '{}'",
                    sourceFile.getAbsolutePath(), destFile.getAbsolutePath());
        } else {
            // Fallback: classic stream copy then delete original
            log.warn("[File#4] renameTo() failed — falling back to copy+delete for '{}'",
                    sourceFile.getName());
            copyFileClassic(sourceFile, destFile);
            boolean deleted = sourceFile.delete();   // File.delete() after copy
            if (!deleted) {
                log.warn("[File#4] Could not delete source after copy: {}",
                        sourceFile.getAbsolutePath());
            }
        }
        return destFile;
    }

    // ================================================================== //
    //  FILE OP #5 — renameDocument                                        //
    //  Uses: File.renameTo(), File.getParentFile()                        //
    // ================================================================== //
    /**
     * Renames a document file in-place within the same directory.
     *
     * @return true if rename succeeded
     */
    public boolean renameDocument(String absoluteFilePath, String newFileName) {
        File oldFile = new File(absoluteFilePath);
        if (!oldFile.exists()) {
            log.warn("[File#5] File to rename does not exist: {}", absoluteFilePath);
            return false;
        }

        // getParentFile() returns the parent directory as a File object
        File newFile = new File(oldFile.getParentFile(), newFileName);

        boolean success = oldFile.renameTo(newFile);   // File.renameTo()
        if (success) {
            log.info("[File#5] Renamed '{}' → '{}'", oldFile.getName(), newFile.getName());
        } else {
            log.warn("[File#5] Rename failed: '{}' → '{}'", oldFile.getName(), newFileName);
        }
        return success;
    }

    // ================================================================== //
    //  FILE OP #6 — deleteFileIfExists                                    //
    //  Uses: File.exists(), File.isFile(), File.delete()                  //
    // ================================================================== //
    /**
     * Deletes a single file using the java.io.File API.
     *
     * @return true if successfully deleted or didn't exist
     */
    public boolean deleteFileIfExists(String absoluteFilePath) {
        File file = new File(absoluteFilePath);

        if (!file.exists()) {
            log.info("[File#6] File does not exist, nothing to delete: {}", absoluteFilePath);
            return true;
        }
        if (!file.isFile()) {
            log.warn("[File#6] Path is not a regular file: {}", absoluteFilePath);
            return false;
        }

        boolean deleted = file.delete();   // File.delete()
        if (deleted) {
            log.info("[File#6] Deleted file: {}", absoluteFilePath);
        } else {
            log.warn("[File#6] Could not delete file (locked?): {}", absoluteFilePath);
        }
        return deleted;
    }

    // ================================================================== //
    //  FILE OP #7 — purgeLoanDirectory                                    //
    //  Uses: File.listFiles() recursion, File.delete(), File.isDirectory() //
    // ================================================================== //
    /**
     * Recursively deletes all files and subdirectories for a loan.
     * Uses File.listFiles() in a recursive helper, then File.delete() on the directory.
     *
     * @return number of files deleted
     */
    public int purgeLoanDirectory(String loanId) {
        File loanDir = new File(uploadBaseDir + File.separator + loanId);
        int[] count  = {0};
        deleteRecursive(loanDir, count);
        log.info("[File#7] Purged {} files from {}", count[0], loanDir.getAbsolutePath());
        return count[0];
    }

    private void deleteRecursive(File file, int[] count) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();   // File.listFiles()
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child, count);
                }
            }
        } else {
            boolean deleted = file.delete();      // File.delete()
            if (deleted) count[0]++;
        }
        file.delete(); // Delete the directory itself after children are gone
    }

    // ================================================================== //
    //  FILE OP #8 — writePropertiesSnapshot                               //
    //  Uses: FileOutputStream, Properties.store()                         //
    // ================================================================== //
    /**
     * Saves a snapshot of a document's metadata as a .properties file alongside
     * the document on disk — useful for system recovery without DB access.
     */
    public void writePropertiesSnapshot(LoanDocument doc) throws IOException {
        File loanDir   = createUploadDirectory(doc.getLoanId());
        File propsFile = new File(loanDir, doc.getStoredFileName() + ".meta.properties");

        Properties props = new Properties();
        props.setProperty("documentId",      doc.getDocumentId());
        props.setProperty("loanId",          doc.getLoanId());
        props.setProperty("customerId",      doc.getCustomerId() != null ? doc.getCustomerId() : "");
        props.setProperty("documentType",    doc.getDocumentType());
        props.setProperty("originalFileName",doc.getOriginalFileName());
        props.setProperty("storedFileName",  doc.getStoredFileName());
        props.setProperty("fileSizeBytes",   String.valueOf(doc.getFileSizeBytes()));
        props.setProperty("mimeType",        doc.getMimeType() != null ? doc.getMimeType() : "");
        props.setProperty("uploadedBy",      doc.getUploadedBy() != null ? doc.getUploadedBy() : "");
        props.setProperty("status",          doc.getStatus() != null ? doc.getStatus() : "PENDING_REVIEW");
        props.setProperty("uploadedAt",      LocalDateTime.now().format(DISPLAY_FMT));

        // FileOutputStream — classic java.io File class usage
        try (FileOutputStream fos = new FileOutputStream(propsFile)) {
            props.store(fos, "Freddie Mac Portal — Document Metadata Snapshot");
        }

        log.info("[File#8] Wrote metadata snapshot: {}", propsFile.getAbsolutePath());
    }

    // ================================================================== //
    //  FILE OP #9 — readPropertiesFromFile                                //
    //  Uses: File.exists(), File.canRead(), FileInputStream               //
    // ================================================================== //
    /**
     * Reads a document metadata .properties file back from disk.
     * Used for recovery when the DB record is missing.
     */
    public Properties readPropertiesFromFile(String propertiesFilePath) throws IOException {
        File propsFile = new File(propertiesFilePath);

        if (!propsFile.exists() || !propsFile.canRead()) {   // File.exists() + File.canRead()
            throw new FileNotFoundException("Properties file not readable: " + propertiesFilePath);
        }

        Properties props = new Properties();
        // FileInputStream — classic java.io
        try (FileInputStream fis = new FileInputStream(propsFile)) {
            props.load(fis);
        }

        log.info("[File#9] Loaded {} properties from {}", props.size(), propsFile.getName());
        return props;
    }

    // ================================================================== //
    //  FILE OP #10 — writeIndexFile                                       //
    //  Uses: File.getParentFile(), FileWriter (append mode), BufferedWriter //
    // ================================================================== //
    /**
     * Appends an entry to a per-loan document index file (index.txt).
     * The index lists every uploaded file with metadata for quick scanning
     * without querying DB2.
     *
     * Uses FileWriter in APPEND mode to never overwrite existing entries.
     */
    public void writeIndexFile(LoanDocument doc) throws IOException {
        File loanDir   = createUploadDirectory(doc.getLoanId());
        File indexFile = new File(loanDir, "index.txt");

        // FileWriter(file, true) → append mode
        try (FileWriter fw = new FileWriter(indexFile, true);
             BufferedWriter bw = new BufferedWriter(fw)) {

            String line = String.join("|",
                    LocalDateTime.now().format(DISPLAY_FMT),
                    doc.getDocumentId(),
                    doc.getDocumentType(),
                    doc.getOriginalFileName(),
                    String.valueOf(doc.getFileSizeBytes()),
                    doc.getStatus() != null ? doc.getStatus() : "PENDING_REVIEW"
            );

            bw.write(line);
            bw.newLine();
            bw.flush();
        }

        log.info("[File#10] Appended index entry for '{}' in {}",
                doc.getOriginalFileName(), indexFile.getAbsolutePath());
    }

    // ================================================================== //
    //  FILE OP #11 — readIndexFile                                        //
    //  Uses: File.exists(), FileReader, BufferedReader                    //
    // ================================================================== //
    /**
     * Reads all lines from a loan's index.txt and returns them as a list.
     * Each entry is a pipe-delimited record appended by writeIndexFile().
     */
    public List<String> readIndexFile(String loanId) throws IOException {
        File indexFile = new File(uploadBaseDir + File.separator + loanId
                + File.separator + "index.txt");

        List<String> lines = new ArrayList<>();

        if (!indexFile.exists()) {                      // File.exists()
            log.info("[File#11] No index file for loanId={}", loanId);
            return lines;
        }

        // FileReader + BufferedReader — classic java.io
        try (FileReader fr = new FileReader(indexFile, StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(fr)) {

            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    lines.add(line.trim());
                }
            }
        }

        log.info("[File#11] Read {} index lines for loanId={}", lines.size(), loanId);
        return lines;
    }

    // ================================================================== //
    //  FILE OP #12 — copyFileClassic (java.io byte-copy)                  //
    //  Uses: FileInputStream, FileOutputStream, read/write loop           //
    // ================================================================== //
    /**
     * Copies a file using classic java.io streams.
     * This is the traditional pre-NIO approach and is used as a
     * fallback when File.renameTo() fails across filesystems.
     */
    public void copyFileClassic(File source, File destination) throws IOException {
        log.info("[File#12] Copying '{}' → '{}'",
                source.getAbsolutePath(), destination.getAbsolutePath());

        // Ensure parent directories exist for destination
        File destParent = destination.getParentFile();
        if (destParent != null && !destParent.exists()) {
            destParent.mkdirs();                   // File.getParentFile() + mkdirs()
        }

        // Classic FileInputStream → FileOutputStream byte-copy
        try (FileInputStream  fis = new FileInputStream(source);
             FileOutputStream fos = new FileOutputStream(destination)) {

            byte[] buffer = new byte[8192]; // 8 KB buffer
            int    bytesRead;
            long   totalCopied = 0;

            while ((bytesRead = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
                totalCopied += bytesRead;
            }
            fos.flush();
            log.info("[File#12] Copied {} bytes from '{}' to '{}'",
                    totalCopied, source.getName(), destination.getName());
        }
    }

    // ================================================================== //
    //  FILE OP #13 — filterFilesByExtension                               //
    //  Uses: File.listFiles(FilenameFilter)                               //
    // ================================================================== //
    /**
     * Returns only files with a given extension from a loan's directory.
     * Demonstrates the {@link FilenameFilter} functional interface.
     *
     * @param extension e.g. "pdf", "png" — without the dot
     */
    public List<File> filterFilesByExtension(String loanId, String extension) {
        File loanDir = new File(uploadBaseDir + File.separator + loanId);

        if (!loanDir.exists() || !loanDir.isDirectory()) {
            return Collections.emptyList();
        }

        // FilenameFilter — java.io functional interface
        FilenameFilter filter = (dir, name) ->
                name.toLowerCase().endsWith("." + extension.toLowerCase());

        File[] matched = loanDir.listFiles(filter);   // File.listFiles(FilenameFilter)
        if (matched == null) return Collections.emptyList();

        log.info("[File#13] Found {} .{} files in loan dir {}",
                matched.length, extension, loanId);
        return Arrays.asList(matched);
    }

    // ================================================================== //
    //  FILE OP #14 — totalSizeOfDirectory                                 //
    //  Uses: File.listFiles() recursive + File.length() accumulation      //
    // ================================================================== //
    /**
     * Calculates the total disk size (bytes) of all files under a loan directory,
     * including all subdirectories, using recursive File.listFiles() traversal.
     */
    public long totalSizeOfDirectory(String loanId) {
        File loanDir = new File(uploadBaseDir + File.separator + loanId);
        long totalBytes = sumDirectory(loanDir);
        log.info("[File#14] Total size of loanId={} directory: {} bytes", loanId, totalBytes);
        return totalBytes;
    }

    private long sumDirectory(File dir) {
        if (!dir.exists()) return 0L;
        if (dir.isFile())  return dir.length();   // File.length()

        long sum  = 0;
        File[] children = dir.listFiles();         // File.listFiles()
        if (children != null) {
            for (File child : children) {
                sum += sumDirectory(child);        // recursive
            }
        }
        return sum;
    }

    // ================================================================== //
    //  FILE OP #15 — ensureDirectoryStructure                             //
    //  Uses: File.mkdirs(), File.isDirectory(), File.getAbsolutePath()    //
    // ================================================================== //
    /**
     * Ensures the full directory structure for the portal exists on startup.
     * Called from a @PostConstruct or startup hook.
     *
     * Created structure:
     *   <uploadBaseDir>/
     *   <reportsBaseDir>/
     *   <reportsBaseDir>/ops-logs/
     *   <reportsBaseDir>/archive/
     */
    public void ensureDirectoryStructure() {
        String[] directories = {
                uploadBaseDir,
                reportsBaseDir,
                reportsBaseDir + File.separator + "ops-logs",
                reportsBaseDir + File.separator + "archive"
        };

        for (String dirPath : directories) {
            File dir = new File(dirPath);
            if (!dir.isDirectory()) {             // File.isDirectory()
                boolean created = dir.mkdirs();   // File.mkdirs()
                log.info("[File#15] {} directory: {}",
                        created ? "Created" : "Failed to create", dir.getAbsolutePath());
            } else {
                log.debug("[File#15] Already exists: {}", dir.getAbsolutePath());
            }
        }
    }

    // ================================================================== //
    //  Utility — human-readable file size using File.length()             //
    // ================================================================== //
    /**
     * Formats a {@link File}'s size into a human-readable string.
     * Accepts a {@code File} object directly (not just a long) to
     * demonstrate File.length().
     */
    public String formatFileSize(File file) {
        if (!file.exists() || !file.isFile()) return "—";
        long bytes = file.length();               // File.length()
        if (bytes < 1_024L)              return bytes + " B";
        if (bytes < 1_048_576L)          return String.format("%.1f KB", bytes / 1_024.0);
        if (bytes < 1_073_741_824L)      return String.format("%.1f MB", bytes / 1_048_576.0);
        return                                   String.format("%.1f GB", bytes / 1_073_741_824.0);
    }
}

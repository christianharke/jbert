package ch.jbert.services;

import ch.jbert.models.Metadata;
import ch.jbert.models.Playlist;
import ch.jbert.models.Track;
import io.micronaut.context.annotation.Value;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

public class PlaylistService extends DataService<Playlist> {

    private static final Logger LOG = LoggerFactory.getLogger(PlaylistService.class);
    private static final String FILE_SUFFIX = ".m3u";

    @Value("${restapi.playlists.path}")
    private String basePath;
    
    @Inject
    private TrackService trackService;

    @Override
    public Playlist create(Playlist playlist) throws IOException {

        if (exists(playlist)) {
            throw new IllegalArgumentException(String.format("Playlist '%s' already exists",
                    playlist.getName().orElse(null)));
        }
        return addTracks(playlist);
    }

    @Override
    public List<Playlist> getAll() {

        LOG.debug("Reading playlists from folder {}", basePath);

        final String[] filenames = new File(basePath).list((dir, name) -> name.endsWith(FILE_SUFFIX));
        if (filenames == null) {
            throw new IllegalStateException(String.format("Given playlists path is not accessible: '%s'",
                    basePath));
        }
        return Arrays.stream(filenames)
                .map(filename -> filename.substring(0, filename.length() - FILE_SUFFIX.length()))
                .map(name -> new Playlist(name, getTracks(name + FILE_SUFFIX)))
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    public List<Playlist> findAllByName(String name) {

        if (name == null || name.isEmpty()) {
            return getAll();
        }

        return getAll().stream()
                .filter(playlist -> playlist.getName().map(n -> n.matches("(?i:.*" + name + ".*)"))
                        .orElse(false))
                .collect(Collectors.toList());
    }

    public Optional<Playlist> findOneByName(String name) {
        return getAll().stream()
                .filter(playlist -> playlist.getName().map(n -> n.equals(name)).orElse(false))
                .findFirst();
    }

    private List<Track> getTracks(String filename) {
        final Path file = Paths.get(basePath, filename);
        if (Files.notExists(file) || !Files.isReadable(file)) {
            throw new IllegalStateException(String.format("Cannot read tracks from playlist file '%s'", filename));
        }

        try (final BufferedReader reader = Files.newBufferedReader(file)) {
            return reader.lines()
                    .map(line -> {
                        try {
                            return trackService.readId3Tags(line);
                        } catch (Exception e) {
                            LOG.info("Could not read ID3 tags from file {}: {}", line, e.getMessage());
                            final List<String> entry = Arrays.asList(line.split("/"));
                            return Metadata.newBuilder()
                                    .withArtist(entry.get(0))
                                    .withAlbum(entry.get(1))
                                    .withTitle(entry.get(2))
                                    .build();
                        }
                    })
                    .map(metadata -> Track.newBuilder().withMetadata(metadata).build())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new IllegalStateException(String.format("Cannot read tracks from playlist file '%s'", filename));
        }
    }

    @Override
    public Playlist update(Playlist original, Playlist update) throws IOException {

        if (!original.getName().equals(update.getName()) && exists(update)) {
            throw new IllegalArgumentException(String.format("Playlist '%s' already exists",
                    update.getName().orElse(null)));
        }
        delete(original);
        return addTracks(update);
    }

    @Override
    public Playlist delete(Playlist playlist) throws IOException {
        final Optional<Path> filePath = getFilePath(playlist);
        if (filePath.isPresent()) {
            Files.deleteIfExists(filePath.get());
        }
        return playlist;
    }

    public Playlist deleteTrackByIndex(Playlist playlist, int index) throws IOException {

        final List<Track> copiedTracks = Collections.emptyList();
        Collections.copy(copiedTracks, playlist.getTracks());

        // Delete track from copied tracks list
        copiedTracks.remove(index);

        final Playlist update = playlist.getName().map(n -> new Playlist(n, copiedTracks)).orElseThrow(
                () -> new IllegalArgumentException(String.format("Could not get name from playlist '%s'", playlist)));

        return update(playlist, update);
    }

    private boolean exists(Playlist playlist) {
        return getFilePath(playlist).map(Files::exists).orElse(false);
    }

    private boolean notExists(Playlist playlist) {
        return !exists(playlist);
    }

    private Playlist addTracks(Playlist playlist) throws IOException {

        final Path file = getFilePath(playlist).orElseThrow(
                () -> new IllegalArgumentException(String.format("Cannot get file path of playlist '%s'", playlist)));

        try (final BufferedWriter writer = Files.newBufferedWriter(file)) {
            for (Track track : playlist.getTracks()) {
                writer.write(trackService.getRelativeFilePath(trackService.create(track)));
                writer.newLine();
            }
        }
        return playlist;
    }

    private Optional<Path> getFilePath(Playlist playlist) {
        return playlist.getName().map(name -> Paths.get(basePath, name + FILE_SUFFIX));
    }
}

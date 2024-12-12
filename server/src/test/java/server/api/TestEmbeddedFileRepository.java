package server.api;

import commons.EmbeddedFile;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;
import server.database.EmbeddedFileRepository;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class TestEmbeddedFileRepository implements EmbeddedFileRepository {
    @Override
    public List<EmbeddedFile> findByNoteId(Long noteId) {
        return null;
    }

    @Override
    public void deleteByNoteId(Long noteId) {

    }

    @Override
    public void flush() {

    }

    @Override
    public <S extends EmbeddedFile> S saveAndFlush(S entity) {
        return null;
    }

    @Override
    public <S extends EmbeddedFile> List<S> saveAllAndFlush(Iterable<S> entities) {
        return null;
    }

    @Override
    public void deleteAllInBatch(Iterable<EmbeddedFile> entities) {

    }

    @Override
    public void deleteAllByIdInBatch(Iterable<Long> longs) {

    }

    @Override
    public void deleteAllInBatch() {

    }

    @Override
    public EmbeddedFile getOne(Long aLong) {
        return null;
    }

    @Override
    public EmbeddedFile getById(Long aLong) {
        return null;
    }

    @Override
    public EmbeddedFile getReferenceById(Long aLong) {
        return null;
    }

    @Override
    public <S extends EmbeddedFile> Optional<S> findOne(Example<S> example) {
        return Optional.empty();
    }

    @Override
    public <S extends EmbeddedFile> List<S> findAll(Example<S> example) {
        return null;
    }

    @Override
    public <S extends EmbeddedFile> List<S> findAll(Example<S> example, Sort sort) {
        return null;
    }

    @Override
    public <S extends EmbeddedFile> Page<S> findAll(Example<S> example, Pageable pageable) {
        return null;
    }

    @Override
    public <S extends EmbeddedFile> long count(Example<S> example) {
        return 0;
    }

    @Override
    public <S extends EmbeddedFile> boolean exists(Example<S> example) {
        return false;
    }

    @Override
    public <S extends EmbeddedFile, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
        return null;
    }

    @Override
    public <S extends EmbeddedFile> S save(S entity) {
        return null;
    }

    @Override
    public <S extends EmbeddedFile> List<S> saveAll(Iterable<S> entities) {
        return null;
    }

    @Override
    public Optional<EmbeddedFile> findById(Long aLong) {
        return Optional.empty();
    }

    @Override
    public boolean existsById(Long aLong) {
        return false;
    }

    @Override
    public List<EmbeddedFile> findAll() {
        return null;
    }

    @Override
    public List<EmbeddedFile> findAllById(Iterable<Long> longs) {
        return null;
    }

    @Override
    public long count() {
        return 0;
    }

    @Override
    public void deleteById(Long aLong) {

    }

    @Override
    public void delete(EmbeddedFile entity) {

    }

    @Override
    public void deleteAllById(Iterable<? extends Long> longs) {

    }

    @Override
    public void deleteAll(Iterable<? extends EmbeddedFile> entities) {

    }

    @Override
    public void deleteAll() {

    }

    @Override
    public List<EmbeddedFile> findAll(Sort sort) {
        return null;
    }

    @Override
    public Page<EmbeddedFile> findAll(Pageable pageable) {
        return null;
    }
}

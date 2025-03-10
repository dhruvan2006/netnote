package server.api;

import commons.Collection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;
import server.database.CollectionRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public class TestCollectionRepository implements CollectionRepository {

    public List<Collection> collections = new ArrayList<>();

    @Override
    public void flush() {

    }

    @Override
    public <S extends Collection> S saveAndFlush(S entity) {
        return null;
    }

    @Override
    public <S extends Collection> List<S> saveAllAndFlush(Iterable<S> entities) {
        return List.of();
    }

    @Override
    public void deleteAllInBatch(Iterable<Collection> entities) {

    }

    @Override
    public void deleteAllByIdInBatch(Iterable<UUID > longs) {

    }

    @Override
    public void deleteAllInBatch() {

    }

    @Override
    public Collection getOne(UUID  aUUID ) {
        return null;
    }

    @Override
    public Collection getById(UUID  aUUID ) {
        return null;
    }

    @Override
    public Collection getReferenceById(UUID  aUUID ) {
        return null;
    }

    @Override
    public <S extends Collection> Optional<S> findOne(Example<S> example) {
        return Optional.empty();
    }

    @Override
    public <S extends Collection> List<S> findAll(Example<S> example) {
        return List.of();
    }

    @Override
    public <S extends Collection> List<S> findAll(Example<S> example, Sort sort) {
        return List.of();
    }

    @Override
    public <S extends Collection> Page<S> findAll(Example<S> example, Pageable pageable) {
        return null;
    }

    @Override
    public <S extends Collection> long count(Example<S> example) {
        return 0;
    }

    @Override
    public <S extends Collection> boolean exists(Example<S> example) {
        return false;
    }

    @Override
    public <S extends Collection, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
        return null;
    }

    @Override
    public <S extends Collection> S save(S entity) throws DataIntegrityViolationException {
        for (Collection collection : collections) {
            if (collection.title.equals(entity.title)) {
                throw new DataIntegrityViolationException("Title already exists");
            }
        }
        for (int i = 0; i < collections.size(); i++) {

            if (collections.get(i).id == entity.id) {
                collections.set(i, entity);
                return entity;
            }
        }
        entity.id = UUID.randomUUID();
        collections.add(entity);
        return entity;
    }

    @Override
    public <S extends Collection> List<S> saveAll(Iterable<S> entities) {
        return List.of();
    }

    @Override
    public Optional<Collection> findById(UUID aUUID ) {
        return collections.stream().filter(e -> e.id.equals(aUUID)).findFirst();
    }

    @Override
    public boolean existsById(UUID  aUUID ) {
        return false;
    }

    @Override
    public List<Collection> findAll() {
        return new ArrayList<>(collections);
    }

    @Override
    public List<Collection> findAllById(Iterable<UUID > longs) {
        return List.of();
    }

    @Override
    public long count() {
        return 0;
    }

    @Override
    public void deleteById(UUID  aUUID ) {
        collections.removeIf(e -> e.id.equals(aUUID) );

    }

    @Override
    public void delete(Collection entity) {

    }

    @Override
    public void deleteAllById(Iterable<? extends UUID > longs) {

    }

    @Override
    public void deleteAll(Iterable<? extends Collection> entities) {

    }

    @Override
    public void deleteAll() {

    }

    @Override
    public List<Collection> findAll(Sort sort) {
        return List.of();
    }

    @Override
    public Page<Collection> findAll(Pageable pageable) {
        return null;
    }

    @Override
    public Collection findByTitle(String title) {
        for (Collection c : collections) {
            if (c.title.equals(title)) {
                return c;
            }
        }
        return null;
    }
}

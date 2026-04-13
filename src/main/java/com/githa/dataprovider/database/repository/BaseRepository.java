package com.githa.dataprovider.database.repository;

import com.githa.dataprovider.database.util.DynamicFilter;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

/**
 * Repositório base abstrato que encapsula o uso do EntityManager.
 * Todos os repositórios concretos devem estender esta classe
 * em vez de implementar PanacheRepository diretamente.
 *
 * @param <T> Tipo da entidade JPA
 */
public abstract class BaseRepository<T> implements PanacheRepository<T> {

    @Inject
    EntityManager entityManager;

    /**
     * Salva ou atualiza a entidade.
     * Se o ID for null, persiste (insert). Caso contrário, faz merge (update).
     *
     * @param entity a entidade a ser salva
     * @param id     o ID da entidade (pode ser null para novas entidades)
     * @return a entidade gerenciada
     */
    public T saveOrUpdate(T entity, Long id) {
        if (id != null) {
            return entityManager.merge(entity);
        } else {
            persist(entity);
            return entity;
        }
    }

    /**
     * Aplica filtros dinâmicos com paginação e ordenação usando JPA Criteria API.
     *
     * @param entityClass a classe da entidade
     * @param params      parâmetros de filtro, paginação e ordenação
     * @return resultado filtrado com dados e total
     */
    public DynamicFilter.FilterResult<T> findWithFilters(
            Class<T> entityClass,
            DynamicFilter.FilterParams params) {
        return DynamicFilter.applyFilters(entityManager, entityClass, params);
    }

    /**
     * Acesso protegido ao EntityManager para queries customizadas
     * que os repositórios concretos possam precisar.
     */
    protected EntityManager getEm() {
        return entityManager;
    }
}

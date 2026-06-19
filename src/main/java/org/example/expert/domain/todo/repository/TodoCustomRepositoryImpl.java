package org.example.expert.domain.todo.repository;


import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.example.expert.domain.manager.entity.QManager;
import org.example.expert.domain.todo.dto.response.TodoSearchResponse;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.user.entity.QUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.example.expert.domain.comment.entity.QComment.comment;
import static org.example.expert.domain.manager.entity.QManager.manager;
import static org.example.expert.domain.todo.entity.QTodo.todo;
import static org.example.expert.domain.user.entity.QUser.user;

public class TodoCustomRepositoryImpl implements TodoCustomRepository {

    private final JPAQueryFactory queryFactory;

    public TodoCustomRepositoryImpl(EntityManager em) { queryFactory = new JPAQueryFactory(em); }

    @Override
    public Optional<Todo> findByIdWithUser(Long todoId) {
        Todo result = queryFactory
                .selectFrom(todo)
                .leftJoin(todo.user, user).fetchJoin()
                .where(todo.id.eq(todoId))
                .fetchOne();

        return Optional.ofNullable(result);
    }

    /*
    contents 쿼리:
    검색 결과 목록을 DTO로 바로 조회

    total 쿼리:
    페이징을 위해 전체 개수 조회

    countDistinct:
    manager/comment 조인 때문에 중복 카운트가 생기지 않게 처리

    managerNicknameContains:
    담당자 닉네임 검색 조건을 exists 서브쿼리로 분리
     */

    @Override
    public Page<TodoSearchResponse> searchTodos(
            String title,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String managerNickname,
            Pageable pageable
    ) {
        List<TodoSearchResponse> contents = queryFactory
                .select(Projections.constructor(
                        TodoSearchResponse.class,
                        todo.title,
                        manager.countDistinct(),
                        comment.countDistinct()
                ))
                .from(todo)
                .leftJoin(todo.managers, manager)
                .leftJoin(todo.comments, comment)
                .where(
                        titleContains(title),
                        createdAtGoe(startDate),
                        createdAtLoe(endDate),
                        managerNicknameContains(managerNickname)
                )
                .groupBy(todo.id, todo.title, todo.createdAt)
                .orderBy(todo.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(todo.countDistinct())
                .from(todo)
                .where(
                        titleContains(title),
                        createdAtGoe(startDate),
                        createdAtLoe(endDate),
                        managerNicknameContains(managerNickname)
                )
                .fetchOne();

        return new PageImpl<>(contents, pageable, total == null ? 0 : total);
    }

    private BooleanExpression titleContains(String title) {
        return title == null || title.isBlank() ? null : todo.title.contains(title);
    }

    private BooleanExpression createdAtGoe(LocalDateTime startDate) {
        return startDate == null ? null : todo.createdAt.goe(startDate);
    }

    private BooleanExpression createdAtLoe(LocalDateTime endDate) {
        return endDate == null ? null : todo.createdAt.loe(endDate);
    }

    private BooleanExpression managerNicknameContains(String managerNickname) {
        if (managerNickname == null || managerNickname.isBlank()) {
            return null;
        }

        QManager searchManager = new QManager("searchManager");
        QUser searchUser = new QUser("searchUser");

        return JPAExpressions
                .selectOne()
                .from(searchManager)
                .join(searchManager.user, searchUser)
                .where(
                        searchManager.todo.eq(todo),
                        searchUser.nickname.contains(managerNickname)
                )
                .exists();
    }
}

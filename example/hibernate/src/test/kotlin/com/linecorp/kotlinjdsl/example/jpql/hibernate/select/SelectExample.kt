package com.linecorp.kotlinjdsl.example.jpql.hibernate.select

import com.linecorp.kotlinjdsl.dsl.jpql.jpql
import com.linecorp.kotlinjdsl.example.jpql.hibernate.HibernateExample
import com.linecorp.kotlinjdsl.example.jpql.hibernate.entity.author.Author
import com.linecorp.kotlinjdsl.example.jpql.hibernate.entity.book.Book
import com.linecorp.kotlinjdsl.example.jpql.hibernate.entity.book.BookAuthor
import com.linecorp.kotlinjdsl.example.jpql.hibernate.entity.book.BookPrice
import com.linecorp.kotlinjdsl.example.jpql.hibernate.entity.book.Isbn
import com.linecorp.kotlinjdsl.example.jpql.hibernate.entity.employee.Employee
import com.linecorp.kotlinjdsl.example.jpql.hibernate.entity.employee.EmployeeDepartment
import java.time.OffsetDateTime
import org.assertj.core.api.WithAssertions
import org.junit.jupiter.api.Test

class SelectExample : WithAssertions, HibernateExample() {

    @Test
    fun `the most prolific author`() {
        // when
        val actual = entityManager.createQuery(
            jpql {
                select(
                    path(Author::authorId),
                ).from(
                    entity(Author::class),
                    join(BookAuthor::class).on(path(Author::authorId).equal(path(BookAuthor::authorId))),
                ).groupBy(
                    path(Author::authorId),
                ).orderBy(
                    count(Author::authorId).desc(),
                )
            },
        ).setMaxResults(1).resultList.first()

        // then
        assertThat(actual).isEqualTo(1L)
    }

    @Test
    fun `authors who haven't written a book`() {
        // when
        val actual = entityManager.createQuery(
            jpql {
                select(
                    path(Author::authorId),
                ).from(
                    entity(Author::class),
                    leftJoin(BookAuthor::class).on(path(Author::authorId).equal(path(BookAuthor::authorId))),
                ).where(
                    path(BookAuthor::authorId).isNull(),
                ).orderBy(
                    path(Author::authorId).asc(),
                )
            },
        ).resultList

        // then
        assertThat(actual).isEqualTo(listOf(4L))
    }

    @Test
    fun books() {
        // when
        val actual = entityManager.createQuery(
            jpql {
                select(
                    path(Book::isbn),
                ).from(
                    entity(Book::class),
                ).orderBy(
                    path(Book::isbn).asc(),
                )
            },
        ).setFirstResult(3).setMaxResults(3).resultList

        // then
        assertThat(actual).isEqualTo(listOf(Isbn("04"), Isbn("05"), Isbn("06")))
    }

    @Test
    fun `the book with the most authors`() {
        // when
        val actual = entityManager.createQuery(
            jpql {
                select(
                    path(Book::isbn),
                ).from(
                    entity(Book::class),
                    join(Book::authors),
                ).groupBy(
                    path(Book::isbn),
                ).orderBy(
                    count(Book::isbn).desc(),
                )
            },
        ).setMaxResults(1).resultList.first()

        // then
        assertThat(actual).isEqualTo(Isbn("01"))
    }

    @Test
    fun `the most expensive book`() {
        // when
        val actual = entityManager.createQuery(
            jpql {
                select(
                    path(Book::isbn),
                ).from(
                    entity(Book::class),
                ).orderBy(
                    path(Book::salePrice).desc(),
                    path(Book::isbn).asc(),
                )
            },
        ).setMaxResults(1).resultList.first()

        // then
        assertThat(actual).isEqualTo(Isbn("10"))
    }

    @Test
    fun `the most recently published book`() {
        // when

        val actual = entityManager.createQuery(
            jpql {
                select(
                    path(Book::isbn),
                ).from(
                    entity(Book::class),
                ).orderBy(
                    path(Book::salePrice).desc(),
                    path(Book::isbn).asc(),
                )
            },
        ).setMaxResults(1).resultList.first()

        // then
        assertThat(actual).isEqualTo(Isbn("10"))
    }

    @Test
    fun `books published between January and June 2023`() {
        // when
        val actual = entityManager.createQuery(
            jpql {
                select(
                    path(Book::isbn),
                ).from(
                    entity(Book::class),
                ).where(
                    path(Book::publishDate).between(
                        OffsetDateTime.parse("2023-01-01T00:00:00+09:00"),
                        OffsetDateTime.parse("2023-06-30T23:59:59+09:00"),
                    ),
                ).orderBy(
                    path(Book::isbn).asc(),
                )
            },
        ).resultList

        // then
        assertThat(actual).isEqualTo(
            listOf(
                Isbn("01"),
                Isbn("02"),
                Isbn("03"),
                Isbn("04"),
                Isbn("05"),
                Isbn("06"),
            ),
        )
    }

    @Test
    fun `the book with the biggest discounts`() {
        // when
        val actual = entityManager.createQuery(
            jpql {
                select(
                    path(Book::isbn),
                ).from(
                    entity(Book::class),
                ).orderBy(
                    path(Book::price)(BookPrice::value).minus(path(Book::salePrice)(BookPrice::value)).desc(),
                )
            },
        ).setMaxResults(1).resultList.first()

        // then
        assertThat(actual).isEqualTo(Isbn("12"))
    }

    @Test
    fun `employees without a nickname`() {
        // when
        val actual = entityManager.createQuery(
            jpql {
                select(
                    path(Employee::employeeId),
                ).from(
                    entity(Employee::class),
                ).where(
                    path(Employee::nickname).isNull(),
                ).orderBy(
                    path(Employee::employeeId).asc(),
                )
            },
        ).resultList

        // then
        assertThat(actual).isEqualTo(
            listOf(
                1L,
                2L,
                3L,
                4L,
                5L,
                6L,
                7L,
                16L,
                17L,
                18L,
                19L,
                20L,
                21L,
                22L,
                23L,
            ),
        )
    }

    @Test
    fun the_number_of_employees_per_department() {
        // given
        data class Row(
            val departmentId: Long,
            val count: Long,
        )

        // when
        val actual = entityManager.createQuery(
            jpql {
                selectNew<Row>(
                    path(EmployeeDepartment::departmentId),
                    count(Employee::employeeId),
                ).from(
                    entity(Employee::class),
                    join(Employee::departments),
                ).groupBy(
                    path(EmployeeDepartment::departmentId),
                ).orderBy(
                    path(EmployeeDepartment::departmentId).asc(),
                )
            },
        ).resultList

        // then
        assertThat(actual).isEqualTo(
            listOf(
                Row(1, 6),
                Row(2, 15),
                Row(3, 18),
            ),
        )
    }

    @Test
    fun `the number of employees who belong to more than one department`() {
        // given
        data class DerivedEntity(
            val employeeId: Long,
            val count: Long,
        )

        // when
        val actual = entityManager.createQuery(
            jpql {
                val subquery = select<DerivedEntity>(
                    path(Employee::employeeId).`as`(expression("employeeId")),
                    count(Employee::employeeId).`as`(expression("count")),
                ).from(
                    entity(Employee::class),
                    join(Employee::departments),
                ).groupBy(
                    path(Employee::employeeId),
                ).having(
                    count(Employee::employeeId).greaterThan(1L),
                )

                select(
                    count(DerivedEntity::employeeId),
                ).from(
                    subquery.asEntity(),
                )
            },
        ).resultList

        // then
        assertThat(actual).isEqualTo(listOf(7L))
    }
}

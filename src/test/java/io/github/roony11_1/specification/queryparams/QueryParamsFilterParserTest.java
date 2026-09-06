package io.github.roony11_1.specification.queryparams;

import io.github.roony11_1.specification.core.FilterCondition;
import io.github.roony11_1.specification.core.FilterConditions;
import io.github.roony11_1.specification.core.FilterException;
import io.github.roony11_1.specification.core.FilterOperator;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QueryParamsFilterParserTest
{
    @Test
    void valorSimpleConOperadorExplicito()
    {
        FilterConditions conditions = QueryParamsFilterParser.parse(
            Map.of("name", "eq:Ricardo")
        );

        assertThat(conditions.getConditions())
            .containsExactly(new FilterCondition("name", FilterOperator.EQ, "Ricardo"));
    }

    @Test
    void valorSimpleSinOperadorEsIgualdad()
    {
        FilterConditions conditions = QueryParamsFilterParser.parse(
            Map.of("name", "Ricardo")
        );

        assertThat(conditions.getConditions())
            .containsExactly(new FilterCondition("name", FilterOperator.EQ, "Ricardo"));
    }

    @Test
    void multiplesParametros()
    {
        FilterConditions conditions = QueryParamsFilterParser.parse(
            Map.of(
                "name", "eq:Ricardo",
                "age", "gte:18",
                "status", "eq:ACTIVE"
            )
        );

        assertThat(conditions.getConditions()).containsExactlyInAnyOrder(
            new FilterCondition("name", FilterOperator.EQ, "Ricardo"),
            new FilterCondition("age", FilterOperator.GTE, "18"),
            new FilterCondition("status", FilterOperator.EQ, "ACTIVE")
        );
    }

    @Test
    void todosLosOperadoresSoportados()
    {
        Map<String, String> params = new HashMap<>();
        params.put("campoEq", "eq:1");
        params.put("campoNe", "ne:1");
        params.put("campoLike", "like:a");
        params.put("campoIlike", "ilike:a");
        params.put("campoGt", "gt:1");
        params.put("campoGte", "gte:1");
        params.put("campoLt", "lt:1");
        params.put("campoLte", "lte:1");
        params.put("campoIn", "in:1,2,3");
        params.put("campoBetween", "between:1,10");
        params.put("campoIsNull", "is_null:true");
        params.put("campoIsNotNull", "is_not_null:true");

        FilterConditions conditions = QueryParamsFilterParser.parse(params);

        assertThat(conditions.getConditions()).containsExactlyInAnyOrder(
            new FilterCondition("campoEq", FilterOperator.EQ, "1"),
            new FilterCondition("campoNe", FilterOperator.NE, "1"),
            new FilterCondition("campoLike", FilterOperator.LIKE, "a"),
            new FilterCondition("campoIlike", FilterOperator.ILIKE, "a"),
            new FilterCondition("campoGt", FilterOperator.GT, "1"),
            new FilterCondition("campoGte", FilterOperator.GTE, "1"),
            new FilterCondition("campoLt", FilterOperator.LT, "1"),
            new FilterCondition("campoLte", FilterOperator.LTE, "1"),
            new FilterCondition("campoIn", FilterOperator.IN, "1,2,3"),
            new FilterCondition("campoBetween", FilterOperator.BETWEEN, "1", "10"),
            new FilterCondition("campoIsNull", FilterOperator.IS_NULL, null),
            new FilterCondition("campoIsNotNull", FilterOperator.IS_NOT_NULL, null)
        );
    }

    @Test
    void soportaSintaxisNativaDelCoreConPipe()
    {
        FilterConditions conditions = QueryParamsFilterParser.parse(
            Map.of(
                "age", "gt|18",
                "id", "in:1,2,3",
                "temp", "between:0,10"
            )
        );

        assertThat(conditions.getConditions()).containsExactlyInAnyOrder(
            new FilterCondition("age", FilterOperator.GT, "18"),
            new FilterCondition("id", FilterOperator.IN, "1,2,3"),
            new FilterCondition("temp", FilterOperator.BETWEEN, "0", "10")
        );
    }

    @Test
    void excluyeParametrosDePaginacion()
    {
        FilterConditions conditions = QueryParamsFilterParser.parse(
            Map.of(
                "page", "0",
                "size", "20",
                "sort", "name",
                "age", "gte:18"
            )
        );

        assertThat(conditions.getConditions())
            .containsExactly(new FilterCondition("age", FilterOperator.GTE, "18"));
    }

    @Test
    void soloParametrosDePaginacionDevuelveVacio()
    {
        FilterConditions conditions = QueryParamsFilterParser.parse(
            Map.of("page", "0", "size", "20", "sort", "name")
        );

        assertThat(conditions.isEmpty()).isTrue();
        assertThat(conditions.getConditions()).isEmpty();
    }

    @Test
    void aplicaAliases()
    {
        FilterConditions conditions = QueryParamsFilterParser.parse(
            Map.of("sucursal", "eq:Centro"),
            Map.of("sucursal", "sucursal.nombre")
        );

        assertThat(conditions.getConditions())
            .containsExactly(new FilterCondition("sucursal.nombre", FilterOperator.EQ, "Centro"));
    }

    @Test
    void sinAliasUtilizaElNombreOriginal()
    {
        FilterConditions conditions = QueryParamsFilterParser.parse(
            Map.of(
                "age", "gte:18",
                "sucursal", "eq:Centro"
            ),
            Map.of("sucursal", "sucursal.nombre")
        );

        assertThat(conditions.getConditions()).containsExactlyInAnyOrder(
            new FilterCondition("age", FilterOperator.GTE, "18"),
            new FilterCondition("sucursal.nombre", FilterOperator.EQ, "Centro")
        );
    }

    @Test
    void aliasConOperadorLike()
    {
        FilterConditions conditions = QueryParamsFilterParser.parse(
            Map.of("sucursal", "like:Centro"),
            Map.of("sucursal", "sucursal.nombre")
        );

        assertThat(conditions.getConditions())
            .containsExactly(new FilterCondition("sucursal.nombre", FilterOperator.LIKE, "Centro"));
    }

    @Test
    void aliasConOperadorBetweenResuelvePathAnidado()
    {
        FilterConditions conditions = QueryParamsFilterParser.parse(
            Map.of("rango", "between:1,10"),
            Map.of("rango", "sucursal.rango")
        );

        assertThat(conditions.getConditions())
            .containsExactly(new FilterCondition("sucursal.rango", FilterOperator.BETWEEN, "1", "10"));
    }

    @Test
    void mapaVacioDevuelveCondicionesVacias()
    {
        FilterConditions conditions = QueryParamsFilterParser.parse(Map.of());

        assertThat(conditions.isEmpty()).isTrue();
        assertThat(conditions.getConditions()).isEmpty();
    }

    @Test
    void mapaNullDevuelveCondicionesVacias()
    {
        FilterConditions conditions = QueryParamsFilterParser.parse(null);

        assertThat(conditions.isEmpty()).isTrue();
        assertThat(conditions.getConditions()).isEmpty();
    }

    @Test
    void aliasesNullSeTrataComoVacio()
    {
        FilterConditions conditions = QueryParamsFilterParser.parse(
            Map.of("age", "gte:18"),
            null
        );

        assertThat(conditions.getConditions())
            .containsExactly(new FilterCondition("age", FilterOperator.GTE, "18"));
    }

    @Test
    void operadorInvalidoPropagaFilterException()
    {
        assertThatThrownBy(() -> QueryParamsFilterParser.parse(
            Map.of("age", "foo:bar")
        ))
            .isInstanceOf(FilterException.class)
            .hasMessageContaining("foo");
    }

    @Test
    void operadorSinValorPropagaFilterException()
    {
        assertThatThrownBy(() -> QueryParamsFilterParser.parse(
            Map.of("age", "gt:")
        ))
            .isInstanceOf(FilterException.class)
            .hasMessageContaining("requiere un valor");
    }

    @Test
    void inVacioPropagaFilterException()
    {
        assertThatThrownBy(() -> QueryParamsFilterParser.parse(
            Map.of("id", "in:")
        ))
            .isInstanceOf(FilterException.class)
            .hasMessageContaining("IN");
    }

    @Test
    void betweenMalformadoPropagaFilterException()
    {
        assertThatThrownBy(() -> QueryParamsFilterParser.parse(
            Map.of("temp", "between:0")
        ))
            .isInstanceOf(FilterException.class)
            .hasMessageContaining("BETWEEN");
    }

    @Test
    void noModificaLosParametrosNiAliases()
    {
        Map<String, String> params = new HashMap<>();
        params.put("age", "gte:18");
        params.put("sucursal", "eq:Centro");
        params.put("page", "0");

        Map<String, String> aliases = new HashMap<>();
        aliases.put("sucursal", "sucursal.nombre");

        QueryParamsFilterParser.parse(params, aliases);

        assertThat(params).containsExactlyInAnyOrderEntriesOf(
            Map.of("age", "gte:18", "sucursal", "eq:Centro", "page", "0")
        );
        assertThat(aliases).containsExactlyInAnyOrderEntriesOf(
            Map.of("sucursal", "sucursal.nombre")
        );
    }
}
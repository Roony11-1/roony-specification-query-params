# roony-specification-query-params

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
![Java](https://img.shields.io/badge/Java-21%2B-blue)

Adaptador de *query params* HTTP de **`roony-specification-core`**. Expone `QueryParamsFilterParser`, que convierte un `Map<String, String>` (por ejemplo, el `query string` de una petición) en `FilterConditions`. Sin dependencias de Spring, JPA ni Jakarta.

## Arquitectura

```text
HTTP/query params
       ↓
roony-specification-query-params → QueryParamsFilterParser (Map<String,String> → FilterConditions) ← ESTE MÓDULO
       ↓
roony-specification-core         → FilterCondition, FilterConditions, FilterOperator, FilterParser, FilterException, ValueConverter
       ↓
roony-specification-jpa          → JpaPredicateBuilder (FilterConditions → Predicate)
       ↓
roony-specification-spring       → FilterSpecificationBuilder (FilterConditions → Specification<T>)
```

La conversión `Map<String, String> → FilterConditions` pertenece únicamente a este módulo; `roony-specification-spring` no la conoce.

## Instalación

```xml
<dependency>
    <groupId>io.github.roony11-1</groupId>
    <artifactId>roony-specification-query-params</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Uso

```java
Map<String, String> params = Map.of(
        "sucursal", "like:Central",
        "precio", "gte:100",
        "estado", "in:ACTIVO,INACTIVO");

Map<String, String> aliases = Map.of("sucursal", "sucursal.nombre");

FilterConditions conditions = QueryParamsFilterParser.parse(params, aliases);

// condiciones listas para roony-specification-jpa / roony-specification-spring
```

## Sintaxis

### Igualdad simple

`?nombre=Juan` → `nombre EQ Juan` (sin operador, el core asume `EQ`).

### Operadores en formato HTTP (`operador:valor`)

El parser traduce la sintaxis HTTP con `:` a la sintaxis nativa del core con `|`:

| Query param | FilterConditions |
|---|---|
| `?precio=gt:1000` | `precio GT 1000` |
| `?estado=neq:INACTIVO` | `estado NE INACTIVO` |
| `?email=like:@gmail.com` | `email LIKE '%@gmail.com%'` |
| `?nombre=ilike:juan` | `nombre ILIKE '%juan%'` |
| `?precio=gte:18` | `precio GTE 18` |
| `?precio=lt:30` | `precio LT 30` |
| `?precio=lte:30` | `precio LTE 30` |
| `?estado=is_null` / `is_not_null` | `estado IS NULL` / `IS NOT NULL` |

También acepta la sintaxis nativa `?precio=gte|100`.

### Operadores multi-valor (passthrough al core)

`in:` y `between:` se pasan tal cual al `FilterParser`, que ya los soporta:

```text
?estado=in:ACTIVO,INACTIVO       → estado IN (ACTIVO, INACTIVO)
?precio=between:100,1000          → precio BETWEEN 100 AND 1000
```

### Parámetros ignorados

`page`, `size` y `sort` se excluyen automáticamente (no generan condiciones).

### Aliases

Con el segundo argumento puedes exponer nombres cortos/protegidos en la API y mapearlos al campo real de la entidad:

```java
FilterConditions conditions = QueryParamsFilterParser.parse(params, Map.of(
    "cat", "categoria",
    "edad", "cliente.edad"
));
```

## API

```java
static FilterConditions parse(Map<String, String> params)
static FilterConditions parse(Map<String, String> params, Map<String, String> aliases)
```

## Errores

`FilterException` se lanza ante operadores inválidos (`foo:bar`), operadores sin valor (`gt:`) o `in:`/`between:` malformados. Los operadores no reconocidos no se corrigen ni se ignoran: fallan explícitamente.

## Dependencias

- `roony-specification-core` (única dependencia)

---

MIT License · [Roony11-1](https://github.com/roony11-1)
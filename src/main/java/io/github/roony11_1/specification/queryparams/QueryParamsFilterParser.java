package io.github.roony11_1.specification.queryparams;

import io.github.roony11_1.specification.core.FilterCondition;
import io.github.roony11_1.specification.core.FilterConditions;
import io.github.roony11_1.specification.core.FilterParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class QueryParamsFilterParser
{
    private static final Set<String> EXCLUDED_PARAMS = Set.of("page", "size", "sort");
    private static final String IN_PREFIX = "in:";
    private static final String BETWEEN_PREFIX = "between:";

    private QueryParamsFilterParser()
    {
    }

    public static FilterConditions parse(Map<String, String> params)
    {
        return parse(params, Map.of());
    }

    public static FilterConditions parse(Map<String, String> params, Map<String, String> aliases)
    {
        FilterConditions conditions = new FilterConditions();

        if (params == null || params.isEmpty())
        {
            return conditions;
        }

        Map<String, String> aliasMap = aliases != null ? aliases : Map.of();

        for (Map.Entry<String, String> entry : params.entrySet())
        {
            String key = entry.getKey();
            if (EXCLUDED_PARAMS.contains(key))
            {
                continue;
            }

            String field = aliasMap.getOrDefault(key, key);
            String value = toCoreSyntax(entry.getValue());

            List<FilterCondition> parsed = new ArrayList<>();
            FilterParser.parseAndAdd(field, value, parsed);
            conditions.addAll(parsed);
        }

        return conditions;
    }

    private static String toCoreSyntax(String value)
    {
        if (value == null || hasNativePrefix(value))
        {
            return value;
        }

        int colonIndex = value.indexOf(':');
        if (colonIndex <= 0)
        {
            return value;
        }

        String operator = value.substring(0, colonIndex);
        if (!operator.matches("[A-Za-z_]+"))
        {
            return value;
        }

        return operator + "|" + value.substring(colonIndex + 1);
    }

    private static boolean hasNativePrefix(String value)
    {
        return value.regionMatches(true, 0, IN_PREFIX, 0, IN_PREFIX.length())
            || value.regionMatches(true, 0, BETWEEN_PREFIX, 0, BETWEEN_PREFIX.length());
    }
}
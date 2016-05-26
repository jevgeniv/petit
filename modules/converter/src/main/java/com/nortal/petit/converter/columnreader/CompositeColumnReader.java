/**
 *   Copyright 2014 Nortal AS
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.nortal.petit.converter.columnreader;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.nortal.petit.converter.Converter;

public class CompositeColumnReader<M, T> implements ColumnReader<T>{
    private final Converter<M, T> converter;
    private final ColumnReader<M> columnReader;

    public CompositeColumnReader(Converter<M, T> converter, ColumnReader<M> strategy) {
        this.converter = converter;
        this.columnReader = strategy;
    }
    
    @Override
    public T getColumnValue(ResultSet rs, int index) throws SQLException {
        return converter.convert(columnReader.getColumnValue(rs, index));
    }
}

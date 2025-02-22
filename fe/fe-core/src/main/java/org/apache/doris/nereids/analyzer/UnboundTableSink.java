// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.apache.doris.nereids.analyzer;

import org.apache.doris.nereids.exceptions.UnboundException;
import org.apache.doris.nereids.memo.GroupExpression;
import org.apache.doris.nereids.properties.LogicalProperties;
import org.apache.doris.nereids.properties.UnboundLogicalProperties;
import org.apache.doris.nereids.trees.expressions.Expression;
import org.apache.doris.nereids.trees.expressions.NamedExpression;
import org.apache.doris.nereids.trees.expressions.Slot;
import org.apache.doris.nereids.trees.plans.BlockFuncDepsPropagation;
import org.apache.doris.nereids.trees.plans.Plan;
import org.apache.doris.nereids.trees.plans.PlanType;
import org.apache.doris.nereids.trees.plans.algebra.Sink;
import org.apache.doris.nereids.trees.plans.commands.info.DMLCommandType;
import org.apache.doris.nereids.trees.plans.logical.LogicalSink;
import org.apache.doris.nereids.trees.plans.visitor.PlanVisitor;
import org.apache.doris.nereids.util.Utils;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Represent an olap table sink plan node that has not been bound.
 */
public class UnboundTableSink<CHILD_TYPE extends Plan> extends LogicalSink<CHILD_TYPE>
        implements Unbound, Sink, BlockFuncDepsPropagation {

    private final List<String> nameParts;
    private final List<String> colNames;
    private final List<String> hints;
    private final boolean temporaryPartition;
    private final List<String> partitions;
    private final boolean isPartialUpdate;
    private final DMLCommandType dmlCommandType;

    public UnboundTableSink(List<String> nameParts, List<String> colNames, List<String> hints,
            List<String> partitions, CHILD_TYPE child) {
        this(nameParts, colNames, hints, false, partitions,
                false, DMLCommandType.NONE, Optional.empty(), Optional.empty(), child);
    }

    public UnboundTableSink(List<String> nameParts, List<String> colNames, List<String> hints,
            boolean temporaryPartition, List<String> partitions, CHILD_TYPE child) {
        this(nameParts, colNames, hints, temporaryPartition, partitions,
                false, DMLCommandType.NONE, Optional.empty(), Optional.empty(), child);
    }

    public UnboundTableSink(List<String> nameParts, List<String> colNames, List<String> hints,
            List<String> partitions, boolean isPartialUpdate, CHILD_TYPE child) {
        this(nameParts, colNames, hints, false, partitions, isPartialUpdate, DMLCommandType.NONE,
                Optional.empty(), Optional.empty(), child);
    }

    public UnboundTableSink(List<String> nameParts, List<String> colNames, List<String> hints,
            boolean temporaryPartition, List<String> partitions, boolean isPartialUpdate, CHILD_TYPE child) {
        this(nameParts, colNames, hints, temporaryPartition, partitions, isPartialUpdate, DMLCommandType.NONE,
                Optional.empty(), Optional.empty(), child);
    }

    public UnboundTableSink(List<String> nameParts, List<String> colNames, List<String> hints,
            boolean temporaryPartition, List<String> partitions,
            boolean isPartialUpdate, DMLCommandType dmlCommandType, CHILD_TYPE child) {
        this(nameParts, colNames, hints, temporaryPartition, partitions, isPartialUpdate, dmlCommandType,
                Optional.empty(), Optional.empty(), child);
    }

    /**
     * constructor
     */
    public UnboundTableSink(List<String> nameParts, List<String> colNames, List<String> hints,
            boolean temporaryPartition, List<String> partitions,
            boolean isPartialUpdate, DMLCommandType dmlCommandType,
            Optional<GroupExpression> groupExpression, Optional<LogicalProperties> logicalProperties,
            CHILD_TYPE child) {
        super(PlanType.LOGICAL_UNBOUND_OLAP_TABLE_SINK, ImmutableList.of(), groupExpression, logicalProperties, child);
        this.nameParts = Utils.copyRequiredList(nameParts);
        this.colNames = Utils.copyRequiredList(colNames);
        this.hints = Utils.copyRequiredList(hints);
        this.temporaryPartition = temporaryPartition;
        this.partitions = Utils.copyRequiredList(partitions);
        this.isPartialUpdate = isPartialUpdate;
        this.dmlCommandType = dmlCommandType;
    }

    public List<String> getColNames() {
        return colNames;
    }

    public List<String> getNameParts() {
        return nameParts;
    }

    public boolean isTemporaryPartition() {
        return temporaryPartition;
    }

    public List<String> getPartitions() {
        return partitions;
    }

    public List<String> getHints() {
        return hints;
    }

    public boolean isPartialUpdate() {
        return isPartialUpdate;
    }

    public DMLCommandType getDMLCommandType() {
        return dmlCommandType;
    }

    @Override
    public Plan withChildren(List<Plan> children) {
        Preconditions.checkArgument(children.size() == 1, "UnboundOlapTableSink only accepts one child");
        return new UnboundTableSink<>(nameParts, colNames, hints, temporaryPartition, partitions, isPartialUpdate,
                dmlCommandType, groupExpression, Optional.empty(), children.get(0));
    }

    @Override
    public UnboundTableSink<CHILD_TYPE> withOutputExprs(List<NamedExpression> outputExprs) {
        throw new UnboundException("could not call withOutputExprs on UnboundTableSink");
    }

    @Override
    public <R, C> R accept(PlanVisitor<R, C> visitor, C context) {
        return visitor.visitUnboundTableSink(this, context);
    }

    @Override
    public List<? extends Expression> getExpressions() {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + " don't support getExpression()");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UnboundTableSink<?> that = (UnboundTableSink<?>) o;
        return Objects.equals(nameParts, that.nameParts)
                && Objects.equals(colNames, that.colNames)
                && Objects.equals(hints, that.hints)
                && Objects.equals(partitions, that.partitions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nameParts, colNames, hints, partitions);
    }

    @Override
    public Plan withGroupExpression(Optional<GroupExpression> groupExpression) {
        return new UnboundTableSink<>(nameParts, colNames, hints, temporaryPartition, partitions, isPartialUpdate,
                dmlCommandType, groupExpression, Optional.of(getLogicalProperties()), child());
    }

    @Override
    public Plan withGroupExprLogicalPropChildren(Optional<GroupExpression> groupExpression,
            Optional<LogicalProperties> logicalProperties, List<Plan> children) {
        return new UnboundTableSink<>(nameParts, colNames, hints, temporaryPartition, partitions,
                isPartialUpdate, dmlCommandType, groupExpression, logicalProperties, children.get(0));
    }

    @Override
    public LogicalProperties computeLogicalProperties() {
        return UnboundLogicalProperties.INSTANCE;
    }

    @Override
    public List<Slot> computeOutput() {
        throw new UnboundException("output");
    }
}

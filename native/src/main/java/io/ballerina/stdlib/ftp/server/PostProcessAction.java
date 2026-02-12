/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.ftp.server;

/**
 * Represents a post-processing action to perform on a file after processing.
 * Can be either DELETE or MOVE action.
 */
public class PostProcessAction {

    public enum ActionType {
        DELETE,
        MOVE
    }

    private final ActionType actionType;
    private final String moveTo;
    private final boolean preserveSubDirs;

    private PostProcessAction(ActionType actionType, String moveTo, boolean preserveSubDirs) {
        this.actionType = actionType;
        this.moveTo = moveTo;
        this.preserveSubDirs = preserveSubDirs;
    }

    /**
     * Creates a DELETE action.
     *
     * @return A PostProcessAction representing DELETE
     */
    public static PostProcessAction delete() {
        return new PostProcessAction(ActionType.DELETE, null, false);
    }

    /**
     * Creates a MOVE action.
     *
     * @param moveTo The destination directory path
     * @param preserveSubDirs Whether to preserve subdirectory structure
     * @return A PostProcessAction representing MOVE
     */
    public static PostProcessAction move(String moveTo, boolean preserveSubDirs) {
        return new PostProcessAction(ActionType.MOVE, moveTo, preserveSubDirs);
    }

    public ActionType getActionType() {
        return actionType;
    }

    public String getMoveTo() {
        return moveTo;
    }

    public boolean isPreserveSubDirs() {
        return preserveSubDirs;
    }

    public boolean isDelete() {
        return actionType == ActionType.DELETE;
    }

    public boolean isMove() {
        return actionType == ActionType.MOVE;
    }

    @Override
    public String toString() {
        if (isDelete()) {
            return "DELETE";
        } else {
            return "MOVE{moveTo='" + moveTo + "', preserveSubDirs=" + preserveSubDirs + "}";
        }
    }
}

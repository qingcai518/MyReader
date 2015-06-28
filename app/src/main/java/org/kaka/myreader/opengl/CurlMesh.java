package org.kaka.myreader.opengl;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.RectF;
import android.opengl.GLUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

public class CurlMesh {

    // Flag for rendering some lines used for developing. Shows
    // curl position and one for the direction from the
    // position given. Comes handy once playing around with different
    // ways for following pointer.
    private static final boolean DRAW_CURL_POSITION = false;
    // Flag for drawing polygon outlines. Using this flag crashes on emulator
    // due to reason unknown to me. Leaving it here anyway as seeing polygon
    // outlines gives good insight how original rectangle is divided.
    private static final boolean DRAW_POLYGON_OUTLINES = false;
    // Flag for texture rendering. While this is likely something you
    // don't want to do it's been used for development purposes as texture
    // rendering is rather slow on emulator.
    private static final boolean DRAW_TEXTURE = true;
    // Flag for enabling shadow rendering.
    private static final boolean DRAW_SHADOW = true;

    // Colors for shadow. Inner one is the color drawn next to surface where
    // shadowed area starts and outer one is color shadow ends to.
    private static final float[] SHADOW_INNER_COLOR = {0f, 0f, 0f, .5f};
    private static final float[] SHADOW_OUTER_COLOR = {0f, 0f, 0f, .0f};

    // Alpha values for front and back facing texture.
    private static final double BACK_FACE_ALPHA = .2f;
    private static final double FRONT_FACE_ALPHA = 1f;
    // Boolean for 'flipping' texture sideways.
    private boolean flipTexture = false;

    // For testing purposes.
    private int mCurlPositionLinesCount;
    private FloatBuffer mCurlPositionLines;

    // Buffers for feeding rasterizer.
    private FloatBuffer verticesBuffer;
    private FloatBuffer coordinatesBuffer;
    private FloatBuffer colorsBuffer;
    private int verticesBufferCountFront;
    private int verticesBufferCountBack;

    private FloatBuffer shadowColorsBuffer;
    private FloatBuffer shadowVerticesBuffer;
    private int dropShadowCount;
    private int mSelfShadowCount;

    // Maximum number of split lines used for creating a curl.
    private int mMaxCurlSplits;

    // Bounding rectangle for this mesh. mRectagle[0] = top-left corner,
    // rectangle[1] = bottom-left, rectangle[2] = top-right and rectangle[3]
    // bottom-right.
    private Vertex[] rectangle = new Vertex[4];

    // One and only texture id.
    private int[] mTextureIds;
    private Bitmap mBitmap;
    private RectF mTextureRect = new RectF();

    // Let's avoid using 'new' as much as possible. Meaning we introduce arrays
    // once here and reuse them on runtime. Doesn't really have very much effect
    // but avoids some garbage collections from happening.
    private Array<Vertex> tempVertices;
    private Array<Vertex> intersections;
    private Array<Vertex> outputVertices;
    private Array<Vertex> rotatedVertices;
    private Array<Double> scanLines;
    private Array<ShadowVertex> tempShadowVertices;
    private Array<ShadowVertex> selfShadowVertices;
    private Array<ShadowVertex> dropShadowVertices;

    /**
     * Constructor for mesh object.
     *
     * @param maxCurlSplits Maximum number curl can be divided into. The bigger the value
     *                      the smoother curl will be. With the cost of having more
     *                      polygons for drawing.
     */
    public CurlMesh(int maxCurlSplits) {
        // There really is no use for 0 splits.
        mMaxCurlSplits = maxCurlSplits < 1 ? 1 : maxCurlSplits;

        scanLines = new Array<>(maxCurlSplits + 2);
        outputVertices = new Array<>(7);
        rotatedVertices = new Array<>(4);
        intersections = new Array<>(2);
        tempVertices = new Array<>(7 + 4);
        for (int i = 0; i < 7 + 4; ++i) {
            tempVertices.add(new Vertex());
        }

        if (DRAW_SHADOW) {
            selfShadowVertices = new Array<>(
                    (mMaxCurlSplits + 2) * 2);
            dropShadowVertices = new Array<>(
                    (mMaxCurlSplits + 2) * 2);
            tempShadowVertices = new Array<>(
                    (mMaxCurlSplits + 2) * 2);
            for (int i = 0; i < (mMaxCurlSplits + 2) * 2; ++i) {
                tempShadowVertices.add(new ShadowVertex());
            }
        }

        // Rectangle consists of 4 vertices. Index 0 = top-left, index 1 =
        // bottom-left, index 2 = top-right and index 3 = bottom-right.
        for (int i = 0; i < 4; ++i) {
            rectangle[i] = new Vertex();
        }
        // Set up shadow penumbra direction to each vertex. We do fake 'self
        // shadow' calculations based on this information.
        rectangle[0].penumbraX = rectangle[1].penumbraX = rectangle[1].penumbraY = rectangle[3].penumbraY = -1;
        rectangle[0].penumbraY = rectangle[2].penumbraX = rectangle[2].penumbraY = rectangle[3].penumbraX = 1;

        if (DRAW_CURL_POSITION) {
            mCurlPositionLinesCount = 3;
            ByteBuffer hvbb = ByteBuffer
                    .allocateDirect(mCurlPositionLinesCount * 2 * 2 * 4);
            hvbb.order(ByteOrder.nativeOrder());
            mCurlPositionLines = hvbb.asFloatBuffer();
            mCurlPositionLines.position(0);
        }

        // There are 4 vertices from bounding rect, max 2 from adding split line
        // to two corners and curl consists of max mMaxCurlSplits lines each
        // outputting 2 vertices.
        int maxVerticesCount = 4 + 2 + (2 * mMaxCurlSplits);
        ByteBuffer vbb = ByteBuffer.allocateDirect(maxVerticesCount * 3 * 4);
        vbb.order(ByteOrder.nativeOrder());
        verticesBuffer = vbb.asFloatBuffer();
        verticesBuffer.position(0);

        if (DRAW_TEXTURE) {
            ByteBuffer tbb = ByteBuffer
                    .allocateDirect(maxVerticesCount * 2 * 4);
            tbb.order(ByteOrder.nativeOrder());
            coordinatesBuffer = tbb.asFloatBuffer();
            coordinatesBuffer.position(0);
        }

        ByteBuffer cbb = ByteBuffer.allocateDirect(maxVerticesCount * 4 * 4);
        cbb.order(ByteOrder.nativeOrder());
        colorsBuffer = cbb.asFloatBuffer();
        colorsBuffer.position(0);

        if (DRAW_SHADOW) {
            int maxShadowVerticesCount = (mMaxCurlSplits + 2) * 2 * 2;
            ByteBuffer scbb = ByteBuffer
                    .allocateDirect(maxShadowVerticesCount * 4 * 4);
            scbb.order(ByteOrder.nativeOrder());
            shadowColorsBuffer = scbb.asFloatBuffer();
            shadowColorsBuffer.position(0);

            ByteBuffer sibb = ByteBuffer
                    .allocateDirect(maxShadowVerticesCount * 3 * 4);
            sibb.order(ByteOrder.nativeOrder());
            shadowVerticesBuffer = sibb.asFloatBuffer();
            shadowVerticesBuffer.position(0);

            dropShadowCount = mSelfShadowCount = 0;
        }
    }

    /**
     * Sets curl for this mesh.
     *
     * @param curlPos Position for curl 'center'. Can be any point on line collinear
     *                to curl.
     * @param curlDir Curl direction, should be normalized.
     * @param radius  Radius of curl.
     */
    public synchronized void curl(PointF curlPos, PointF curlDir, double radius) {

        // First add some 'helper' lines used for development.
        if (DRAW_CURL_POSITION) {
            mCurlPositionLines.position(0);

            mCurlPositionLines.put(curlPos.x);
            mCurlPositionLines.put(curlPos.y - 1.0f);
            mCurlPositionLines.put(curlPos.x);
            mCurlPositionLines.put(curlPos.y + 1.0f);
            mCurlPositionLines.put(curlPos.x - 1.0f);
            mCurlPositionLines.put(curlPos.y);
            mCurlPositionLines.put(curlPos.x + 1.0f);
            mCurlPositionLines.put(curlPos.y);

            mCurlPositionLines.put(curlPos.x);
            mCurlPositionLines.put(curlPos.y);
            mCurlPositionLines.put(curlPos.x + curlDir.x * 2);
            mCurlPositionLines.put(curlPos.y + curlDir.y * 2);

            mCurlPositionLines.position(0);
        }

        // Actual 'curl' implementation starts here.
        verticesBuffer.position(0);
        colorsBuffer.position(0);
        if (DRAW_TEXTURE) {
            coordinatesBuffer.position(0);
        }

        // Calculate curl angle from direction.
        double curlAngle = Math.acos(curlDir.x);
        curlAngle = curlDir.y > 0 ? -curlAngle : curlAngle;

        // Initiate rotated rectangle which's is translated to curlPos and
        // rotated so that curl direction heads to right (1,0). Vertices are
        // ordered in ascending order based on x -coordinate at the same time.
        // And using y -coordinate in very rare case in which two vertices have
        // same x -coordinate.
        tempVertices.addAll(rotatedVertices);
        rotatedVertices.clear();
        for (int i = 0; i < 4; ++i) {
            Vertex v = tempVertices.remove(0);
            v.set(rectangle[i]);
            v.translate(-curlPos.x, -curlPos.y);
            v.rotateZ(-curlAngle);
            int j = 0;
            for (; j < rotatedVertices.size(); ++j) {
                Vertex v2 = rotatedVertices.get(j);
                if (v.posX > v2.posX) {
                    break;
                }
                if (v.posX == v2.posX && v.posY > v2.posY) {
                    break;
                }
            }
            rotatedVertices.add(j, v);
        }

        // Rotated rectangle lines/vertex indices. We need to find bounding
        // lines for rotated rectangle. After sorting vertices according to
        // their x -coordinate we don't have to worry about vertices at indices
        // 0 and 1. But due to inaccuracy it's possible vertex 3 is not the
        // opposing corner from vertex 0. So we are calculating distance from
        // vertex 0 to vertices 2 and 3 - and altering line indices if needed.
        // Also vertices/lines are given in an order first one has x -coordinate
        // at least the latter one. This property is used in getIntersections to
        // see if there is an intersection.
        int lines[][] = {{0, 1}, {0, 2}, {1, 3}, {2, 3}};
        {
            // TODO: There really has to be more 'easier' way of doing this -
            // not including extensive use of sqrt.
            Vertex v0 = rotatedVertices.get(0);
            Vertex v2 = rotatedVertices.get(2);
            Vertex v3 = rotatedVertices.get(3);
            double dist2 = Math.sqrt((v0.posX - v2.posX)
                    * (v0.posX - v2.posX) + (v0.posY - v2.posY)
                    * (v0.posY - v2.posY));
            double dist3 = Math.sqrt((v0.posX - v3.posX)
                    * (v0.posX - v3.posX) + (v0.posY - v3.posY)
                    * (v0.posY - v3.posY));
            if (dist2 > dist3) {
                lines[1][1] = 3;
                lines[2][1] = 2;
            }
        }

        verticesBufferCountFront = verticesBufferCountBack = 0;

        if (DRAW_SHADOW) {
            tempShadowVertices.addAll(dropShadowVertices);
            tempShadowVertices.addAll(selfShadowVertices);
            dropShadowVertices.clear();
            selfShadowVertices.clear();
        }

        // Length of 'curl' curve.
        double curlLength = Math.PI * radius;
        // Calculate scan lines.
        // TODO: Revisit this code one day. There is room for optimization here.
        scanLines.clear();
        if (mMaxCurlSplits > 0) {
            scanLines.add((double) 0);
        }
        for (int i = 1; i < mMaxCurlSplits; ++i) {
            scanLines.add((-curlLength * i) / (mMaxCurlSplits - 1));
        }
        // As rotatedVertices is ordered regarding x -coordinate, adding
        // this scan line produces scan area picking up vertices which are
        // rotated completely. One could say 'until infinity'.
        scanLines.add(rotatedVertices.get(3).posX - 1);

        // Start from right most vertex. Pretty much the same as first scan area
        // is starting from 'infinity'.
        double scanXmax = rotatedVertices.get(0).posX + 1;

        for (int i = 0; i < scanLines.size(); ++i) {
            // Once we have scanXmin and scanXmax we have a scan area to start
            // working with.
            double scanXmin = scanLines.get(i);
            // First iterate 'original' rectangle vertices within scan area.
            for (int j = 0; j < rotatedVertices.size(); ++j) {
                Vertex v = rotatedVertices.get(j);
                // Test if vertex lies within this scan area.
                // TODO: Frankly speaking, can't remember why equality check was
                // added to both ends. Guessing it was somehow related to case
                // where radius=0f, which, given current implementation, could
                // be handled much more effectively anyway.
                if (v.posX >= scanXmin && v.posX <= scanXmax) {
                    // Pop out a vertex from setting_top vertices.
                    Vertex n = tempVertices.remove(0);
                    n.set(v);
                    // This is done solely for triangulation reasons. Given a
                    // rotated rectangle it has max 2 vertices having
                    // intersection.
                    Array<Vertex> intersections = getIntersections(
                            rotatedVertices, lines, n.posX);
                    // In a sense one could say we're adding vertices always in
                    // two, positioned at the ends of intersecting line. And for
                    // triangulation to work properly they are added based on y
                    // -coordinate. And this if-else is doing it for us.
                    if (intersections.size() == 1
                            && intersections.get(0).posY > v.posY) {
                        // In case intersecting vertex is higher add it first.
                        outputVertices.addAll(intersections);
                        outputVertices.add(n);
                    } else if (intersections.size() <= 1) {
                        // Otherwise add original vertex first.
                        outputVertices.add(n);
                        outputVertices.addAll(intersections);
                    } else {
                        // There should never be more than 1 intersecting
                        // vertex. But if it happens as a fallback simply skip
                        // everything.
                        tempVertices.add(n);
                        tempVertices.addAll(intersections);
                    }
                }
            }

            // Search for scan line intersections.
            Array<Vertex> intersections = getIntersections(rotatedVertices,
                    lines, scanXmin);

            // We expect to get 0 or 2 vertices. In rare cases there's only one
            // but in general given a scan line intersecting rectangle there
            // should be 2 intersecting vertices.
            if (intersections.size() == 2) {
                // There were two intersections, add them based on y
                // -coordinate, higher first, lower last.
                Vertex v1 = intersections.get(0);
                Vertex v2 = intersections.get(1);
                if (v1.posY < v2.posY) {
                    outputVertices.add(v2);
                    outputVertices.add(v1);
                } else {
                    outputVertices.addAll(intersections);
                }
            } else if (intersections.size() != 0) {
                // This happens in a case in which there is a original vertex
                // exactly at scan line or something went very much wrong if
                // there are 3+ vertices. What ever the reason just return the
                // vertices to setting_top vertices for later use. In former case it
                // was handled already earlier once iterating through
                // rotatedVertices, in latter case it's better to avoid doing
                // anything with them.
                tempVertices.addAll(intersections);
            }

            // Add vertices found during this iteration to vertex etc buffers.
            while (outputVertices.size() > 0) {
                Vertex v = outputVertices.remove(0);
                tempVertices.add(v);

                // Untouched vertices.
                if (i == 0) {
                    v.alpha = flipTexture ? BACK_FACE_ALPHA : FRONT_FACE_ALPHA;
                    verticesBufferCountFront++;
                }
                // 'Completely' rotated vertices.
                else if (i == scanLines.size() - 1 || curlLength == 0) {
                    v.posX = -(curlLength + v.posX);
                    v.posZ = 2 * radius;
                    v.penumbraX = -v.penumbraX;

                    v.alpha = flipTexture ? FRONT_FACE_ALPHA : BACK_FACE_ALPHA;
                    verticesBufferCountBack++;
                }
                // Vertex lies within 'curl'.
                else {
                    // Even though it's not obvious from the if-else clause,
                    // here v.posX is between [-curlLength, 0]. And we can do
                    // calculations around a half cylinder.
                    double rotY = Math.PI * (v.posX / curlLength);
                    v.posX = radius * Math.sin(rotY);
                    v.posZ = radius - (radius * Math.cos(rotY));
                    v.penumbraX *= Math.cos(rotY);
                    // Map color multiplier to [.1f, 1f] range.
                    v.color = .1f + .9f * Math.sqrt(Math.sin(rotY) + 1);

                    if (v.posZ >= radius) {
                        v.alpha = flipTexture ? FRONT_FACE_ALPHA
                                : BACK_FACE_ALPHA;
                        verticesBufferCountBack++;
                    } else {
                        v.alpha = flipTexture ? BACK_FACE_ALPHA
                                : FRONT_FACE_ALPHA;
                        verticesBufferCountFront++;
                    }
                }

                // Move vertex back to 'world' coordinates.
                v.rotateZ(curlAngle);
                v.translate(curlPos.x, curlPos.y);
                addVertex(v);

                // Drop shadow is cast 'behind' the curl.
                if (DRAW_SHADOW && v.posZ > 0 && v.posZ <= radius) {
                    ShadowVertex sv = tempShadowVertices.remove(0);
                    sv.posX = v.posX;
                    sv.posY = v.posY;
                    sv.posZ = v.posZ;
                    sv.penumbraX = (v.posZ / 2) * -curlDir.x;
                    sv.penumbraY = (v.posZ / 2) * -curlDir.y;
                    sv.mPenumbraColor = v.posZ / radius;
                    int idx = (dropShadowVertices.size() + 1) / 2;
                    dropShadowVertices.add(idx, sv);
                }
                // Self shadow is cast partly over mesh.
                if (DRAW_SHADOW && v.posZ > radius) {
                    ShadowVertex sv = tempShadowVertices.remove(0);
                    sv.posX = v.posX;
                    sv.posY = v.posY;
                    sv.posZ = v.posZ;
                    sv.penumbraX = ((v.posZ - radius) / 3) * v.penumbraX;
                    sv.penumbraY = ((v.posZ - radius) / 3) * v.penumbraY;
                    sv.mPenumbraColor = (v.posZ - radius) / (2 * radius);
                    int idx = (selfShadowVertices.size() + 1) / 2;
                    selfShadowVertices.add(idx, sv);
                }
            }

            // Switch scanXmin as scanXmax for next iteration.
            scanXmax = scanXmin;
        }

        verticesBuffer.position(0);
        colorsBuffer.position(0);
        if (DRAW_TEXTURE) {
            coordinatesBuffer.position(0);
        }

        // Add shadow Vertices.
        if (DRAW_SHADOW) {
            shadowColorsBuffer.position(0);
            shadowVerticesBuffer.position(0);
            dropShadowCount = 0;

            for (int i = 0; i < dropShadowVertices.size(); ++i) {
                ShadowVertex sv = dropShadowVertices.get(i);
                shadowVerticesBuffer.put((float) sv.posX);
                shadowVerticesBuffer.put((float) sv.posY);
                shadowVerticesBuffer.put((float) sv.posZ);
                shadowVerticesBuffer.put((float) (sv.posX + sv.penumbraX));
                shadowVerticesBuffer.put((float) (sv.posY + sv.penumbraY));
                shadowVerticesBuffer.put((float) sv.posZ);
                for (int j = 0; j < 4; ++j) {
                    double color = SHADOW_OUTER_COLOR[j]
                            + (SHADOW_INNER_COLOR[j] - SHADOW_OUTER_COLOR[j])
                            * sv.mPenumbraColor;
                    shadowColorsBuffer.put((float) color);
                }
                shadowColorsBuffer.put(SHADOW_OUTER_COLOR);
                dropShadowCount += 2;
            }
            mSelfShadowCount = 0;
            for (int i = 0; i < selfShadowVertices.size(); ++i) {
                ShadowVertex sv = selfShadowVertices.get(i);
                shadowVerticesBuffer.put((float) sv.posX);
                shadowVerticesBuffer.put((float) sv.posY);
                shadowVerticesBuffer.put((float) sv.posZ);
                shadowVerticesBuffer.put((float) (sv.posX + sv.penumbraX));
                shadowVerticesBuffer.put((float) (sv.posY + sv.penumbraY));
                shadowVerticesBuffer.put((float) sv.posZ);
                for (int j = 0; j < 4; ++j) {
                    double color = SHADOW_OUTER_COLOR[j]
                            + (SHADOW_INNER_COLOR[j] - SHADOW_OUTER_COLOR[j])
                            * sv.mPenumbraColor;
                    shadowColorsBuffer.put((float) color);
                }
                shadowColorsBuffer.put(SHADOW_OUTER_COLOR);
                mSelfShadowCount += 2;
            }
            shadowColorsBuffer.position(0);
            shadowVerticesBuffer.position(0);
        }
    }

    /**
     * Draws our mesh.
     */
    public synchronized void draw(GL10 gl) {
        // First allocate texture if there is not one yet.
        if (DRAW_TEXTURE) {
            if (mTextureIds == null) {
                // Generate texture.
                mTextureIds = new int[1];
                gl.glGenTextures(1, mTextureIds, 0);
                // Set texture attributes.
                gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureIds[0]);
                gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER,
                        GL10.GL_LINEAR);
                gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER,
                        GL10.GL_LINEAR);
                gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S,
                        GL10.GL_CLAMP_TO_EDGE);
                gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T,
                        GL10.GL_CLAMP_TO_EDGE);
            }
            // If mBitmap != null we have a new texture.
            if (mBitmap != null) {
                gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureIds[0]);
                GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, mBitmap, 0);
                mBitmap = null;
            }

            gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureIds[0]);
        }

        // Some 'global' settings.
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);

        // TODO: Drop shadow drawing is done temporarily here to hide some
        // problems with its calculation.
        if (DRAW_SHADOW) {
            gl.glEnable(GL10.GL_BLEND);
            gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
            gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
            gl.glColorPointer(4, GL10.GL_FLOAT, 0, shadowColorsBuffer);
            gl.glVertexPointer(3, GL10.GL_FLOAT, 0, shadowVerticesBuffer);
            gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, dropShadowCount);
            gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
            gl.glDisable(GL10.GL_BLEND);
        }

        // Enable texture coordinates.
        if (DRAW_TEXTURE) {
            gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
            gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, coordinatesBuffer);
        }
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, verticesBuffer);

        // Enable color array.
        gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
        gl.glColorPointer(4, GL10.GL_FLOAT, 0, colorsBuffer);

        // Draw blank / 'white' front facing vertices.
        gl.glDisable(GL10.GL_TEXTURE_2D);
        gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, verticesBufferCountFront);
        // Draw front facing texture.
        if (DRAW_TEXTURE) {
            gl.glEnable(GL10.GL_BLEND);
            gl.glEnable(GL10.GL_TEXTURE_2D);
            gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
            gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, verticesBufferCountFront);
            gl.glDisable(GL10.GL_TEXTURE_2D);
            gl.glDisable(GL10.GL_BLEND);
        }
        int backStartIdx = Math.max(0, verticesBufferCountFront - 2);
        int backCount = verticesBufferCountFront + verticesBufferCountBack - backStartIdx;
        // Draw blank / 'white' back facing vertices.;
        gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, backStartIdx, backCount);
        // Draw back facing texture.
        if (DRAW_TEXTURE) {
            gl.glEnable(GL10.GL_BLEND);
            gl.glEnable(GL10.GL_TEXTURE_2D);
            gl.glBlendFunc(GL10.GL_ONE_MINUS_SRC_ALPHA, GL10.GL_SRC_ALPHA);
            gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, backStartIdx, backCount);
            gl.glDisable(GL10.GL_TEXTURE_2D);
            gl.glDisable(GL10.GL_BLEND);
        }

        // Disable textures and color array.
        gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
        gl.glDisableClientState(GL10.GL_COLOR_ARRAY);

        if (DRAW_POLYGON_OUTLINES) {
            gl.glEnable(GL10.GL_BLEND);
            gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
            gl.glLineWidth(1.0f);
            gl.glColor4f(0.5f, 0.5f, 1.0f, 1.0f);
            gl.glVertexPointer(3, GL10.GL_FLOAT, 0, verticesBuffer);
            gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, verticesBufferCountFront);
            gl.glDisable(GL10.GL_BLEND);
        }

        if (DRAW_CURL_POSITION) {
            gl.glEnable(GL10.GL_BLEND);
            gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
            gl.glLineWidth(1.0f);
            gl.glColor4f(1.0f, 0.5f, 0.5f, 1.0f);
            gl.glVertexPointer(2, GL10.GL_FLOAT, 0, mCurlPositionLines);
            gl.glDrawArrays(GL10.GL_LINES, 0, mCurlPositionLinesCount * 2);
            gl.glDisable(GL10.GL_BLEND);
        }

        if (DRAW_SHADOW) {
            gl.glEnable(GL10.GL_BLEND);
            gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
            gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
            gl.glColorPointer(4, GL10.GL_FLOAT, 0, shadowColorsBuffer);
            gl.glVertexPointer(3, GL10.GL_FLOAT, 0, shadowVerticesBuffer);
            gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, dropShadowCount,
                    mSelfShadowCount);
            gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
            gl.glDisable(GL10.GL_BLEND);
        }

        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    }

    /**
     * Resets mesh to 'initial' state. Meaning this mesh will draw a plain
     * textured rectangle after call to this method.
     */
    public synchronized void reset() {
        verticesBuffer.position(0);
        colorsBuffer.position(0);
        if (DRAW_TEXTURE) {
            coordinatesBuffer.position(0);
        }
        for (int i = 0; i < 4; ++i) {
            addVertex(rectangle[i]);
        }
        verticesBufferCountFront = 4;
        verticesBufferCountBack = 0;
        verticesBuffer.position(0);
        colorsBuffer.position(0);
        if (DRAW_TEXTURE) {
            coordinatesBuffer.position(0);
        }

        dropShadowCount = mSelfShadowCount = 0;
    }

    /**
     * Resets allocated texture id forcing creation of new one. After calling
     * this method you most likely want to set bitmap too as it's lost. This
     * method should be called only once e.g GL context is re-created as this
     * method does not release previous texture id, only makes sure new one is
     * requested on next render.
     */
    public synchronized void resetTexture() {
        mTextureIds = null;
    }

    /**
     * Sets new texture for this mesh.
     */
    public synchronized void setBitmap(Bitmap bitmap) {
        if (DRAW_TEXTURE) {
            // Bitmap original size.
            int w = bitmap.getWidth();
            int h = bitmap.getHeight();
            // Bitmap size expanded to next power of two. This is done due to
            // the requirement on many devices, texture width and height should
            // be power of two.
            int newW = getNextHighestPO2(w);
            int newH = getNextHighestPO2(h);
            // TODO: Is there another way to create a bigger Bitmap and copy
            // original Bitmap to it more efficiently? Immutable bitmap anyone?
            mBitmap = Bitmap.createBitmap(newW, newH, bitmap.getConfig());
            Canvas c = new Canvas(mBitmap);
            c.drawBitmap(bitmap, 0, 0, null);

            // Calculate final texture coordinates.
            float texX = (float) w / newW;
            float texY = (float) h / newH;
            mTextureRect.set(0f, 0f, texX, texY);
            if (flipTexture) {
                setTexCoords(texX, 0f, 0f, texY);
            } else {
                setTexCoords(0f, 0f, texX, texY);
            }
        }
    }

    /**
     * If true, flips texture sideways.
     */
    public synchronized void setFlipTexture(boolean flipTexture) {

        if (flipTexture) {
            setTexCoords(mTextureRect.right, mTextureRect.top,
                    mTextureRect.left, mTextureRect.bottom);
        } else {
            setTexCoords(mTextureRect.left, mTextureRect.top,
                    mTextureRect.right, mTextureRect.bottom);
        }

        for (int i = 0; i < 4; ++i) {
            rectangle[i].alpha = flipTexture ? BACK_FACE_ALPHA
                    : FRONT_FACE_ALPHA;
        }
    }

    /**
     * Update mesh bounds.
     */
    public void setRect(RectF r) {
        rectangle[0].posX = r.left;
        rectangle[0].posY = r.top;
        rectangle[1].posX = r.left;
        rectangle[1].posY = r.bottom;
        rectangle[2].posX = r.right;
        rectangle[2].posY = r.top;
        rectangle[3].posX = r.right;
        rectangle[3].posY = r.bottom;
    }

    /**
     * Adds vertex to buffers.
     */
    private void addVertex(Vertex vertex) {
        verticesBuffer.put((float) vertex.posX);
        verticesBuffer.put((float) vertex.posY);
        verticesBuffer.put((float) vertex.posZ);
        colorsBuffer.put((float) vertex.color);
        colorsBuffer.put((float) vertex.color);
        colorsBuffer.put((float) vertex.color);
        colorsBuffer.put((float) vertex.alpha);
        if (DRAW_TEXTURE) {
            coordinatesBuffer.put((float) vertex.texX);
            coordinatesBuffer.put((float) vertex.texY);
        }
    }

    /**
     * Calculates intersections for given scan line.
     */
    private Array<Vertex> getIntersections(Array<Vertex> vertices,
                                           int[][] lineIndices, double scanX) {
        intersections.clear();
        // Iterate through rectangle lines each re-presented as a pair of
        // vertices.
        for (int j = 0; j < lineIndices.length; j++) {
            Vertex v1 = vertices.get(lineIndices[j][0]);
            Vertex v2 = vertices.get(lineIndices[j][1]);
            // Here we expect that v1.posX >= v2.posX and wont do intersection
            // test the opposite way.
            if (v1.posX > scanX && v2.posX < scanX) {
                // There is an intersection, calculate coefficient telling 'how
                // far' scanX is from v2.
                double c = (scanX - v2.posX) / (v1.posX - v2.posX);
                Vertex n = tempVertices.remove(0);
                n.set(v2);
                n.posX = scanX;
                n.posY += (v1.posY - v2.posY) * c;
                if (DRAW_TEXTURE) {
                    n.texX += (v1.texX - v2.texX) * c;
                    n.texY += (v1.texY - v2.texY) * c;
                }
                if (DRAW_SHADOW) {
                    n.penumbraX += (v1.penumbraX - v2.penumbraX) * c;
                    n.penumbraY += (v1.penumbraY - v2.penumbraY) * c;
                }
                intersections.add(n);
            }
        }
        return intersections;
    }

    /**
     * Calculates the next highest power of two for a given integer.
     */
    private int getNextHighestPO2(int n) {
        n -= 1;
        n = n | (n >> 1);
        n = n | (n >> 2);
        n = n | (n >> 4);
        n = n | (n >> 8);
        n = n | (n >> 16);
        n = n | (n >> 32);
        return n + 1;
    }

    /**
     * Sets texture coordinates to rectangle vertices.
     */
    private synchronized void setTexCoords(float left, float top, float right,
                                           float bottom) {
        rectangle[0].texX = left;
        rectangle[0].texY = top;
        rectangle[1].texX = left;
        rectangle[1].texY = bottom;
        rectangle[2].texX = right;
        rectangle[2].texY = top;
        rectangle[3].texX = right;
        rectangle[3].texY = bottom;
    }

    /**
     * Simple fixed size array implementation.
     */
    private class Array<T> {
        private Object[] mArray;
        private int mSize;
        private int mCapacity;

        public Array(int capacity) {
            mCapacity = capacity;
            mArray = new Object[capacity];
        }

        public void add(int index, T item) {
            if (index < 0 || index > mSize || mSize >= mCapacity) {
                throw new IndexOutOfBoundsException();
            }
            for (int i = mSize; i > index; --i) {
                mArray[i] = mArray[i - 1];
            }
            mArray[index] = item;
            ++mSize;
        }

        public void add(T item) {
            if (mSize >= mCapacity) {
                throw new IndexOutOfBoundsException();
            }
            mArray[mSize++] = item;
        }

        public void addAll(Array<T> array) {
            if (mSize + array.size() > mCapacity) {
                throw new IndexOutOfBoundsException();
            }
            for (int i = 0; i < array.size(); ++i) {
                mArray[mSize++] = array.get(i);
            }
        }

        public void clear() {
            mSize = 0;
        }

        @SuppressWarnings("unchecked")
        public T get(int index) {
            if (index < 0 || index >= mSize) {
                throw new IndexOutOfBoundsException();
            }
            return (T) mArray[index];
        }

        @SuppressWarnings("unchecked")
        public T remove(int index) {
            if (index < 0 || index >= mSize) {
                throw new IndexOutOfBoundsException();
            }
            T item = (T) mArray[index];
            for (int i = index; i < mSize - 1; ++i) {
                mArray[i] = mArray[i + 1];
            }
            --mSize;
            return item;
        }

        public int size() {
            return mSize;
        }

    }

    /**
     * Holder for shadow vertex information.
     */
    private class ShadowVertex {
        public double posX;
        public double posY;
        public double posZ;
        public double penumbraX;
        public double penumbraY;
        public double mPenumbraColor;
    }

    /**
     * Holder for vertex information.
     */
    private class Vertex {
        public double posX;
        public double posY;
        public double posZ;
        public double texX;
        public double texY;
        public double penumbraX;
        public double penumbraY;
        public double color;
        public double alpha;

        public Vertex() {
            posX = posY = posZ = texX = texY = 0;
            color = alpha = 1;
        }

        public void rotateZ(double theta) {
            double cos = Math.cos(theta);
            double sin = Math.sin(theta);
            double x = posX * cos + posY * sin;
            double y = posX * -sin + posY * cos;
            posX = x;
            posY = y;
            double px = penumbraX * cos + penumbraY * sin;
            double py = penumbraX * -sin + penumbraY * cos;
            penumbraX = px;
            penumbraY = py;
        }

        public void set(Vertex vertex) {
            posX = vertex.posX;
            posY = vertex.posY;
            posZ = vertex.posZ;
            texX = vertex.texX;
            texY = vertex.texY;
            penumbraX = vertex.penumbraX;
            penumbraY = vertex.penumbraY;
            color = vertex.color;
            alpha = vertex.alpha;
        }

        public void translate(double dx, double dy) {
            posX += dx;
            posY += dy;
        }
    }
}

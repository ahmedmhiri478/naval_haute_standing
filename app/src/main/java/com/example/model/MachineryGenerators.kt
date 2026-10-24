package com.example.model

import kotlin.math.*

/**
 * Procedural CAD Generator for Naval Machinery 3D models.
 * Generates accurate vertex-face meshes with realistic naval engineering proportions.
 */
object MachineryGenerators {

    /**
     * Marine Propeller (CPP / FPP)
     * @param blades Number of blades (e.g., 3, 4, 5)
     * @param diameterMm Overall propeller diameter in mm
     * @param pitchRatio Pitch to diameter ratio P/D (typical 0.8 - 1.4)
     * @param rakeDeg Blade rake angle in degrees (aft tilt)
     * @param skewDeg Blade skew angle in degrees (vibration reduction)
     */
    fun generatePropeller(
        blades: Int = 4,
        diameterMm: Float = 2400f,
        pitchRatio: Float = 1.05f,
        rakeDeg: Float = 8f,
        skewDeg: Float = 22f
    ): Mesh3D {
        val vertices = mutableListOf<Point3D>()
        val faces = mutableListOf<Face3D>()

        val scale = 100f / diameterMm // Normalized display scale
        val hubRadius = (diameterMm * 0.14f) * scale
        val hubLength = (diameterMm * 0.35f) * scale
        val tipRadius = (diameterMm * 0.5f) * scale

        // 1. Generate Hub (Cylinder + Nose Cone)
        val hubSegments = 24
        val hubZStart = -hubLength * 0.5f
        val hubZEnd = hubLength * 0.5f

        // Aft ring (z = hubZStart)
        val aftRingStartIdx = vertices.size
        for (i in 0 until hubSegments) {
            val angle = 2f * Math.PI.toFloat() * i / hubSegments
            val r = hubRadius * 0.95f
            vertices.add(Point3D(r * cos(angle), r * sin(angle), hubZStart))
        }

        // Fore ring (z = hubZEnd)
        val foreRingStartIdx = vertices.size
        for (i in 0 until hubSegments) {
            val angle = 2f * Math.PI.toFloat() * i / hubSegments
            val r = hubRadius * 1.05f
            vertices.add(Point3D(r * cos(angle), r * sin(angle), hubZEnd))
        }

        // Hub cylinder side faces
        for (i in 0 until hubSegments) {
            val next = (i + 1) % hubSegments
            faces.add(Face3D(aftRingStartIdx + i, foreRingStartIdx + i, foreRingStartIdx + next))
            faces.add(Face3D(aftRingStartIdx + i, foreRingStartIdx + next, aftRingStartIdx + next))
        }

        // Nose Cone tip (Aft spinner cap)
        val noseTipIdx = vertices.size
        vertices.add(Point3D(0f, 0f, hubZStart - hubRadius * 1.3f))
        for (i in 0 until hubSegments) {
            val next = (i + 1) % hubSegments
            faces.add(Face3D(aftRingStartIdx + next, aftRingStartIdx + i, noseTipIdx))
        }

        // Hub Fore end plate (Center shaft coupling)
        val foreCenterIdx = vertices.size
        vertices.add(Point3D(0f, 0f, hubZEnd))
        for (i in 0 until hubSegments) {
            val next = (i + 1) % hubSegments
            faces.add(Face3D(foreRingStartIdx + i, foreRingStartIdx + next, foreCenterIdx))
        }

        // 2. Generate Propeller Blades with Foil Section, Pitch Twist & Skew
        val bladeRadialSections = 7
        val bladeChordPoints = 5

        for (b in 0 until blades) {
            val bladeBaseAngle = (2f * Math.PI.toFloat() * b / blades)
            val gridIndices = Array(bladeRadialSections) { IntArray(bladeChordPoints) }

            for (r in 0 until bladeRadialSections) {
                val fracR = r.toFloat() / (bladeRadialSections - 1)
                val currentRadius = hubRadius + fracR * (tipRadius - hubRadius)
                // Pitch twist: pitch angle phi = atan(P / (2 * pi * r))
                val localPitchAngle = atan(pitchRatio * diameterMm * scale / (2f * Math.PI.toFloat() * currentRadius))
                // Skew & rake shift
                val skewRad = (skewDeg * (fracR.pow(1.5f))) * (Math.PI.toFloat() / 180f)
                val rakeOffsetZ = (sin(rakeDeg * Math.PI.toFloat() / 180f) * (currentRadius - hubRadius))

                // Chord length variation (Wageningen B-series style distribution)
                val chordMax = (tipRadius - hubRadius) * 0.65f
                val localChord = chordMax * (1.1f * sin(fracR * Math.PI.toFloat()).coerceAtLeast(0.25f))

                for (c in 0 until bladeChordPoints) {
                    val fracC = (c.toFloat() / (bladeChordPoints - 1)) - 0.5f // -0.5 to +0.5
                    val chordX = fracC * localChord
                    // NACA-like thickness distribution
                    val thickness = (0.12f * (1f - fracR * 0.7f) * localChord) * (1f - (2f * fracC).pow(2)).coerceAtLeast(0.04f)

                    // Rotate along pitch angle
                    val pX = chordX * cos(localPitchAngle) - thickness * sin(localPitchAngle)
                    val pZ = chordX * sin(localPitchAngle) + thickness * cos(localPitchAngle) + rakeOffsetZ

                    // Rotate to blade position and apply skew
                    val totalAngle = bladeBaseAngle + skewRad + (pX / currentRadius)
                    val finalX = currentRadius * cos(totalAngle)
                    val finalY = currentRadius * sin(totalAngle)
                    val finalZ = pZ

                    val idx = vertices.size
                    vertices.add(Point3D(finalX, finalY, finalZ))
                    gridIndices[r][c] = idx
                }
            }

            // Create blade faces
            for (r in 0 until bladeRadialSections - 1) {
                for (c in 0 until bladeChordPoints - 1) {
                    val p00 = gridIndices[r][c]
                    val p10 = gridIndices[r + 1][c]
                    val p11 = gridIndices[r + 1][c + 1]
                    val p01 = gridIndices[r][c + 1]

                    // Pressure face
                    faces.add(Face3D(p00, p10, p11))
                    faces.add(Face3D(p00, p11, p01))
                    // Suction reverse face (double sided for 3D solid thickness)
                    faces.add(Face3D(p00, p11, p10))
                    faces.add(Face3D(p00, p01, p11))
                }
            }
        }

        computeFaceNormals(vertices, faces)
        return Mesh3D(
            name = "CPP Marine Propeller (Dia $diameterMm mm)",
            vertices = vertices,
            faces = faces,
            volumeM3 = 0.85f * (diameterMm / 2400f).pow(3),
            massKg = 6460f * (diameterMm / 2400f).pow(3)
        )
    }

    /**
     * Propulsion Shaft Assembly
     * Marine hollow propulsion shaft with coupling flange, stern tube white metal bearing, and keyway
     */
    fun generatePropulsionShafting(
        lengthMm: Float = 5500f,
        outerDiaMm: Float = 380f,
        innerBoreMm: Float = 140f
    ): Mesh3D {
        val vertices = mutableListOf<Point3D>()
        val faces = mutableListOf<Face3D>()

        val scale = 120f / lengthMm
        val rOuter = outerDiaMm * 0.5f * scale
        val rBore = innerBoreMm * 0.5f * scale
        val rFlange = rOuter * 1.9f
        val flangeThickness = lengthMm * 0.08f * scale
        val totalLength = lengthMm * scale
        val segs = 20

        // Sections along Z:
        // z = 0: Flange fore end
        // z = flangeThickness: Flange back
        // z = totalLength * 0.45: Shaft forward
        // z = totalLength * 0.70: Stern tube bearing sleeve start
        // z = totalLength * 0.90: Stern tube bearing sleeve end
        // z = totalLength: Propeller taper end

        val zStations = listOf(
            0f to rFlange,
            flangeThickness to rFlange,
            flangeThickness to rOuter,
            totalLength * 0.45f to rOuter,
            totalLength * 0.55f to (rOuter * 1.15f), // Stern bearing sleeve
            totalLength * 0.85f to (rOuter * 1.15f),
            totalLength * 0.90f to rOuter,
            totalLength to (rOuter * 0.85f)          // Taper for propeller hub
        )

        val stationIndices = mutableListOf<IntArray>()

        for ((z, r) in zStations) {
            val ring = IntArray(segs)
            for (i in 0 until segs) {
                val theta = 2f * Math.PI.toFloat() * i / segs
                val idx = vertices.size
                vertices.add(Point3D(r * cos(theta), r * sin(theta), z - totalLength * 0.5f))
                ring[i] = idx
            }
            stationIndices.add(ring)
        }

        // Bridge adjacent rings
        for (s in 0 until stationIndices.size - 1) {
            val ringA = stationIndices[s]
            val ringB = stationIndices[s + 1]
            for (i in 0 until segs) {
                val next = (i + 1) % segs
                faces.add(Face3D(ringA[i], ringB[i], ringB[next]))
                faces.add(Face3D(ringA[i], ringB[next], ringA[next]))
            }
        }

        // Fore Flange face with inner bore
        val foreCenter = vertices.size
        vertices.add(Point3D(0f, 0f, -totalLength * 0.5f))
        for (i in 0 until segs) {
            val next = (i + 1) % segs
            faces.add(Face3D(stationIndices.first()[next], stationIndices.first()[i], foreCenter))
        }

        // Aft end face
        val aftCenter = vertices.size
        vertices.add(Point3D(0f, 0f, totalLength * 0.5f))
        for (i in 0 until segs) {
            val next = (i + 1) % segs
            faces.add(Face3D(stationIndices.last()[i], stationIndices.last()[next], aftCenter))
        }

        // Add 8 Coupling Flange Bolts
        val boltRadius = (rFlange + rOuter) * 0.5f
        for (b in 0 until 8) {
            val angle = 2f * Math.PI.toFloat() * b / 8
            val bx = boltRadius * cos(angle)
            val by = boltRadius * sin(angle)
            val bz = -totalLength * 0.5f - 4f
            val bIdx = vertices.size
            vertices.add(Point3D(bx, by, bz))
            // Pin visual triangle
            val rIdx = stationIndices[0][(b * segs / 8) % segs]
            faces.add(Face3D(rIdx, bIdx, foreCenter))
        }

        computeFaceNormals(vertices, faces)
        return Mesh3D(
            name = "Propulsion Shaft Assembly (L $lengthMm mm)",
            vertices = vertices,
            faces = faces,
            volumeM3 = 0.52f,
            massKg = 4080f
        )
    }

    /**
     * Marine Diesel Engine Cylinder Head
     * Heavy-fuel two/four stroke engine cylinder cover with central injector bore,
     * twin exhaust valves, cooling water passage, and cylinder head studs
     */
    fun generateDieselCylinderHead(
        boreMm: Float = 480f,
        heightMm: Float = 550f
    ): Mesh3D {
        val vertices = mutableListOf<Point3D>()
        val faces = mutableListOf<Face3D>()

        val scale = 80f / boreMm
        val rHead = boreMm * 0.85f * scale
        val rFiredeck = boreMm * 0.5f * scale
        val h = heightMm * scale
        val segs = 18

        // Lower Firedeck ring (combustion chamber interface)
        val firedeckRing = IntArray(segs)
        for (i in 0 until segs) {
            val th = 2f * Math.PI.toFloat() * i / segs
            firedeckRing[i] = vertices.size
            vertices.add(Point3D(rFiredeck * cos(th), rFiredeck * sin(th), -h * 0.5f))
        }

        // Middle cooling jacket ring
        val midRing = IntArray(segs)
        for (i in 0 until segs) {
            val th = 2f * Math.PI.toFloat() * i / segs
            midRing[i] = vertices.size
            vertices.add(Point3D(rHead * cos(th), rHead * sin(th), 0f))
        }

        // Top cover plate ring
        val topRing = IntArray(segs)
        for (i in 0 until segs) {
            val th = 2f * Math.PI.toFloat() * i / segs
            topRing[i] = vertices.size
            vertices.add(Point3D(rHead * cos(th), rHead * sin(th), h * 0.5f))
        }

        // Connect rings
        for (i in 0 until segs) {
            val next = (i + 1) % segs
            faces.add(Face3D(firedeckRing[i], midRing[i], midRing[next]))
            faces.add(Face3D(firedeckRing[i], midRing[next], firedeckRing[next]))

            faces.add(Face3D(midRing[i], topRing[i], topRing[next]))
            faces.add(Face3D(midRing[i], topRing[next], midRing[next]))
        }

        // Bottom firedeck face with central injector indentation
        val injectorCenter = vertices.size
        vertices.add(Point3D(0f, 0f, -h * 0.5f + 8f)) // recessed injector pocket
        for (i in 0 until segs) {
            val next = (i + 1) % segs
            faces.add(Face3D(firedeckRing[next], firedeckRing[i], injectorCenter))
        }

        // Top surface with Valve Ports & Injector Bore
        val topCenter = vertices.size
        vertices.add(Point3D(0f, 0f, h * 0.5f))
        for (i in 0 until segs) {
            val next = (i + 1) % segs
            faces.add(Face3D(topRing[i], topRing[next], topCenter))
        }

        // Add 2 Twin Exhaust Valve Bosses on Top
        val valveOffset = rHead * 0.42f
        val valveRadius = rHead * 0.28f
        val valveV1 = vertices.size
        vertices.add(Point3D(-valveOffset, 0f, h * 0.5f + 12f))
        val valveV2 = vertices.size
        vertices.add(Point3D(valveOffset, 0f, h * 0.5f + 12f))

        for (i in 0 until 8) {
            val th = 2f * Math.PI.toFloat() * i / 8
            val nextTh = 2f * Math.PI.toFloat() * (i + 1) / 8
            val p1 = vertices.size
            vertices.add(Point3D(-valveOffset + valveRadius * cos(th), valveRadius * sin(th), h * 0.5f))
            val p2 = vertices.size
            vertices.add(Point3D(-valveOffset + valveRadius * cos(nextTh), valveRadius * sin(nextTh), h * 0.5f))
            faces.add(Face3D(p1, p2, valveV1))

            val p3 = vertices.size
            vertices.add(Point3D(valveOffset + valveRadius * cos(th), valveRadius * sin(th), h * 0.5f))
            val p4 = vertices.size
            vertices.add(Point3D(valveOffset + valveRadius * cos(nextTh), valveRadius * sin(nextTh), h * 0.5f))
            faces.add(Face3D(p3, p4, valveV2))
        }

        computeFaceNormals(vertices, faces)
        return Mesh3D(
            name = "Marine Diesel Cylinder Head (Bore $boreMm mm)",
            vertices = vertices,
            faces = faces,
            volumeM3 = 0.28f,
            massKg = 2016f
        )
    }

    /**
     * Marine Propulsion Double-Helical Reduction Gearbox
     * Heavy duty bull gear with helical tooth profile and high-torque hub
     */
    fun generateReductionGearbox(
        gearRatio: Float = 3.8f,
        moduleMm: Float = 16f,
        teethCount: Int = 54
    ): Mesh3D {
        val vertices = mutableListOf<Point3D>()
        val faces = mutableListOf<Face3D>()

        val pitchRadius = (teethCount * moduleMm * 0.5f) * 0.09f // scaled for viewport
        val rootRadius = pitchRadius * 0.88f
        val tipRadius = pitchRadius * 1.08f
        val hubRadius = pitchRadius * 0.35f
        val faceWidth = pitchRadius * 0.7f

        val teeth = teethCount.coerceIn(16, 36)
        val frontZ = -faceWidth * 0.5f
        val backZ = faceWidth * 0.5f

        // Central bore / hub
        val hubSegs = teeth * 2
        val frontHubRing = IntArray(hubSegs)
        val backHubRing = IntArray(hubSegs)

        for (i in 0 until hubSegs) {
            val th = 2f * Math.PI.toFloat() * i / hubSegs
            frontHubRing[i] = vertices.size
            vertices.add(Point3D(hubRadius * cos(th), hubRadius * sin(th), frontZ))
            backHubRing[i] = vertices.size
            vertices.add(Point3D(hubRadius * cos(th), hubRadius * sin(th), backZ))
        }

        // Gear teeth rim profile
        val frontRim = IntArray(hubSegs)
        val backRim = IntArray(hubSegs)

        for (i in 0 until hubSegs) {
            val th = 2f * Math.PI.toFloat() * i / hubSegs
            val isTooth = (i % 2 == 0)
            val r = if (isTooth) tipRadius else rootRadius
            // Herringbone / double helical twist angle
            val frontAngle = th - 0.05f
            val backAngle = th + 0.05f

            frontRim[i] = vertices.size
            vertices.add(Point3D(r * cos(frontAngle), r * sin(frontAngle), frontZ))
            backRim[i] = vertices.size
            vertices.add(Point3D(r * cos(backAngle), r * sin(backAngle), backZ))
        }

        // Bridge teeth outer faces
        for (i in 0 until hubSegs) {
            val next = (i + 1) % hubSegs
            faces.add(Face3D(frontRim[i], backRim[i], backRim[next]))
            faces.add(Face3D(frontRim[i], backRim[next], frontRim[next]))

            // Front gear web face
            faces.add(Face3D(frontHubRing[i], frontRim[i], frontRim[next]))
            faces.add(Face3D(frontHubRing[i], frontRim[next], frontHubRing[next]))

            // Back gear web face
            faces.add(Face3D(backHubRing[i], backRim[next], backRim[i]))
            faces.add(Face3D(backHubRing[i], backHubRing[next], backRim[next]))

            // Hub inner cylinder
            faces.add(Face3D(frontHubRing[i], backHubRing[next], backHubRing[i]))
            faces.add(Face3D(frontHubRing[i], frontHubRing[next], backHubRing[next]))
        }

        computeFaceNormals(vertices, faces)
        return Mesh3D(
            name = "Double-Helical Bull Gear ($teeth teeth, Ratio $gearRatio:1)",
            vertices = vertices,
            faces = faces,
            volumeM3 = 0.45f,
            massKg = 3530f
        )
    }

    /**
     * Marine Centrifugal Ballast & Bilge Pump Impeller
     * Curved backward hydrodynamic vanes, suction eye, and shroud disc
     */
    fun generateCentrifugalPump(
        impellerDiaMm: Float = 450f,
        vaneCount: Int = 6
    ): Mesh3D {
        val vertices = mutableListOf<Point3D>()
        val faces = mutableListOf<Face3D>()

        val scale = 80f / impellerDiaMm
        val rOuter = impellerDiaMm * 0.5f * scale
        val rEye = rOuter * 0.38f
        val rHub = rOuter * 0.18f
        val depth = rOuter * 0.35f
        val segs = 24

        // Backplate disc
        val backCenter = vertices.size
        vertices.add(Point3D(0f, 0f, -depth * 0.5f))

        val backPlateRing = IntArray(segs)
        for (i in 0 until segs) {
            val th = 2f * Math.PI.toFloat() * i / segs
            backPlateRing[i] = vertices.size
            vertices.add(Point3D(rOuter * cos(th), rOuter * sin(th), -depth * 0.5f))
        }

        for (i in 0 until segs) {
            val next = (i + 1) % segs
            faces.add(Face3D(backPlateRing[i], backPlateRing[next], backCenter))
        }

        // Curved Backward Impeller Vanes (Logarithmic spiral profile)
        for (v in 0 until vaneCount) {
            val startAngle = 2f * Math.PI.toFloat() * v / vaneCount
            val vanePts = 6
            val vaneIndicesLow = IntArray(vanePts)
            val vaneIndicesHigh = IntArray(vanePts)

            for (p in 0 until vanePts) {
                val frac = p.toFloat() / (vanePts - 1)
                val r = rEye + frac * (rOuter - rEye)
                // Backward curve theta(r) = startAngle - k * ln(r / rEye)
                val curveAngle = startAngle - (frac.pow(1.3f) * 1.1f)
                val vx = r * cos(curveAngle)
                val vy = r * sin(curveAngle)

                vaneIndicesLow[p] = vertices.size
                vertices.add(Point3D(vx, vy, -depth * 0.5f))

                vaneIndicesHigh[p] = vertices.size
                vertices.add(Point3D(vx, vy, depth * 0.5f))
            }

            for (p in 0 until vanePts - 1) {
                faces.add(Face3D(vaneIndicesLow[p], vaneIndicesHigh[p], vaneIndicesHigh[p + 1]))
                faces.add(Face3D(vaneIndicesLow[p], vaneIndicesHigh[p + 1], vaneIndicesLow[p + 1]))
                // Reverse face for double-sided rendering
                faces.add(Face3D(vaneIndicesLow[p], vaneIndicesHigh[p + 1], vaneIndicesHigh[p]))
                faces.add(Face3D(vaneIndicesLow[p], vaneIndicesLow[p + 1], vaneIndicesHigh[p + 1]))
            }
        }

        // Front Shroud ring with suction eye opening
        val frontShroudRing = IntArray(segs)
        val frontEyeRing = IntArray(segs)
        for (i in 0 until segs) {
            val th = 2f * Math.PI.toFloat() * i / segs
            frontShroudRing[i] = vertices.size
            vertices.add(Point3D(rOuter * cos(th), rOuter * sin(th), depth * 0.5f))
            frontEyeRing[i] = vertices.size
            vertices.add(Point3D(rEye * cos(th), rEye * sin(th), depth * 0.5f))
        }

        for (i in 0 until segs) {
            val next = (i + 1) % segs
            faces.add(Face3D(frontEyeRing[i], frontShroudRing[i], frontShroudRing[next]))
            faces.add(Face3D(frontEyeRing[i], frontShroudRing[next], frontEyeRing[next]))
        }

        computeFaceNormals(vertices, faces)
        return Mesh3D(
            name = "Ballast Pump Impeller (Dia $impellerDiaMm mm)",
            vertices = vertices,
            faces = faces,
            volumeM3 = 0.08f,
            massKg = 608f
        )
    }

    /**
     * Watertight Subdivision Bulkhead & Ship Hull Framing
     * Plated structural bulkhead with bulb-flat vertical stiffeners and deep horizontal stringer
     */
    fun generateWatertightBulkhead(
        widthM: Float = 8.0f,
        heightM: Float = 6.0f,
        plateThicknessMm: Float = 14f
    ): Mesh3D {
        val vertices = mutableListOf<Point3D>()
        val faces = mutableListOf<Face3D>()

        val w = 90f
        val h = 70f
        val t = 3f

        val gridCols = 8
        val gridRows = 6
        val grid = Array(gridRows + 1) { IntArray(gridCols + 1) }

        // Bulkhead plated skin
        for (r in 0..gridRows) {
            val y = -h * 0.5f + (r.toFloat() / gridRows) * h
            for (c in 0..gridCols) {
                val x = -w * 0.5f + (c.toFloat() / gridCols) * w
                val idx = vertices.size
                vertices.add(Point3D(x, y, 0f))
                grid[r][c] = idx
            }
        }

        for (r in 0 until gridRows) {
            for (c in 0 until gridCols) {
                val p00 = grid[r][c]
                val p10 = grid[r + 1][c]
                val p11 = grid[r + 1][c + 1]
                val p01 = grid[r][c + 1]

                faces.add(Face3D(p00, p10, p11))
                faces.add(Face3D(p00, p11, p01))
            }
        }

        // Add 4 Vertical Bulb Stiffeners protruding out in Z
        for (stiff in 1..4) {
            val col = stiff * (gridCols / 5)
            val stiffX = -w * 0.5f + (col.toFloat() / gridCols) * w
            val stiffZ = 12f // Stiffener depth

            for (r in 0 until gridRows) {
                val y1 = -h * 0.5f + (r.toFloat() / gridRows) * h
                val y2 = -h * 0.5f + ((r + 1).toFloat() / gridRows) * h

                val vTop1 = vertices.size
                vertices.add(Point3D(stiffX, y1, stiffZ))
                val vTop2 = vertices.size
                vertices.add(Point3D(stiffX, y2, stiffZ))

                val base1 = grid[r][col]
                val base2 = grid[r + 1][col]

                faces.add(Face3D(base1, vTop1, vTop2))
                faces.add(Face3D(base1, vTop2, base2))
            }
        }

        // Deep Horizontal Web Stringer at 1/3 height (maximum hydrostatic load)
        val stringerRow = gridRows / 3
        val stringerY = -h * 0.5f + (stringerRow.toFloat() / gridRows) * h
        val stringerZ = 18f

        for (c in 0 until gridCols) {
            val x1 = -w * 0.5f + (c.toFloat() / gridCols) * w
            val x2 = -w * 0.5f + ((c + 1).toFloat() / gridCols) * w

            val sTop1 = vertices.size
            vertices.add(Point3D(x1, stringerY, stringerZ))
            val sTop2 = vertices.size
            vertices.add(Point3D(x2, stringerY, stringerZ))

            val b1 = grid[stringerRow][c]
            val b2 = grid[stringerRow][c + 1]

            faces.add(Face3D(b1, sTop1, sTop2))
            faces.add(Face3D(b1, sTop2, b2))
        }

        computeFaceNormals(vertices, faces)
        return Mesh3D(
            name = "Watertight Bulkhead (${widthM}m x ${heightM}m, $plateThicknessMm mm plate)",
            vertices = vertices,
            faces = faces,
            volumeM3 = widthM * heightM * (plateThicknessMm / 1000f),
            massKg = widthM * heightM * (plateThicknessMm / 1000f) * 7850f
        )
    }

    /**
     * Computes surface normal vectors for Gouraud shading and lighting
     */
    private fun computeFaceNormals(vertices: List<Point3D>, faces: List<Face3D>) {
        for (f in faces) {
            if (f.v1 < vertices.size && f.v2 < vertices.size && f.v3 < vertices.size) {
                val p1 = vertices[f.v1]
                val p2 = vertices[f.v2]
                val p3 = vertices[f.v3]
                val edge1 = p2 - p1
                val edge2 = p3 - p1
                f.normal = edge1.cross(edge2).normalized()
            }
        }
    }
}

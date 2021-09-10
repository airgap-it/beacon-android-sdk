package it.airgap.beaconsdk.transport.p2p.matrix.internal.utils

import it.airgap.beaconsdk.core.internal.migration.Migration
import it.airgap.beaconsdk.transport.p2p.matrix.internal.migration.ExtendedMigration
import it.airgap.beaconsdk.transport.p2p.matrix.internal.migration.MatrixMigration

internal fun Migration.extend(): ExtendedMigration = MatrixMigration(this)
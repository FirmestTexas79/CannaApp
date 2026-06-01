package cz.cannaclub.cannaapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.cannaclub.cannaapp.model.User
import cz.cannaclub.cannaapp.ui.theme.CardDefault
import cz.cannaclub.cannaapp.ui.theme.CardHover
import cz.cannaclub.cannaapp.ui.theme.Cream
import cz.cannaclub.cannaapp.ui.theme.Sage
import cz.cannaclub.cannaapp.ui.theme.TextMuted

/**
 * Sdílená pill komponenta pro zákazníka — používána jak v admin seznamu,
 * tak případně v zákaznickém flow. Kanonická verze, bez duplicit.
 */
@Composable
fun UserPillComponent(
    user: User,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(CardDefault)
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar s iniciálami
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(CardHover),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text  = user.initials,
                style = MaterialTheme.typography.bodyLarge,
                color = Cream
            )
        }

        Spacer(modifier = Modifier.width(13.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = user.name,
                style = MaterialTheme.typography.bodyLarge,
                color = Cream
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text  = user.email,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text  = user.points.toString(),
                style = MaterialTheme.typography.headlineMedium,
                color = Sage
            )
            Text(
                text  = "bodů",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = TextMuted
            )
        }
    }
}
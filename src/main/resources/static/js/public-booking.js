document.querySelectorAll('input[name="startTime"]').forEach(function (slot) {
    slot.addEventListener('change', function () {
        const label = slot.closest('.slot-option');
        const start = label.querySelector('strong').textContent.trim();
        const end = label.querySelector('small').textContent.replace(' bitiş', '').trim();
        document.getElementById('selectedSlotText').textContent = start + ' – ' + end;
        document.getElementById('selectedSlotSummary').classList.add('is-selected');
        const firstName = document.getElementById('firstName');
        if (firstName && !firstName.value) {
            firstName.focus({preventScroll: true});
            firstName.scrollIntoView({behavior: 'smooth', block: 'center'});
        }
    });
});

document.getElementById('uploadForm').addEventListener('submit', function(event) {
    event.preventDefault(); // Prevent form from submitting traditionally

    const fileInput = document.getElementById('fileInput');
    const file = fileInput.files[0];
    if (!file) {
        alert('Please select a file!');
        return;
    }

    // Create FormData object to send the file
    const formData = new FormData();
    formData.append('file', file);

    // AJAX request
    fetch('/ffe-cronjob/mass-update-from-excel', {
        method: 'POST',
        body: formData
    })
    .then(response => {
        if (!response.ok) {
            $("<p>Error: " + response +"</p>").appendTo('#errorMessage');
        }
        return response.json();
    })
    .then(data => {
       console.log(data.failedJobs);
       const failedJobs = data.failedJobs;
       failedJobs.forEach(job => {
        $("<p>" + job +"</p>").appendTo('#errorMessage');
       });
        const zipFileContent = atob(data.zipFile);
        const binaryData = new Uint8Array(zipFileContent.length);
        for (let i = 0; i < zipFileContent.length; i++) {
            binaryData[i] = zipFileContent.charCodeAt(i);
        }

        // Create a Blob and trigger the download
        const blob = new Blob([binaryData], { type: 'application/zip' });
        const url = URL.createObjectURL(blob);

        //Creating the curren date to give the zip file a better name
        let currentDate = new Date().toLocaleDateString('en-GB').replace(/\//g, '-');

        // Create a link element to trigger download
        const link = document.createElement('a');
        link.href = url;
        link.download = 'massCronJobScheduleUpdate-' + currentDate + '.zip';
        document.body.appendChild(link);
        link.click();
    })
    .catch(error => {
        console.error('There was an error:', error);
    });
});
import axios from 'axios';

const uploadCourseFile = async (courseId, file) => {
    try {
        const formData = new FormData();
        formData.append('file', file);

        const response = await axios.post(
            `/api/course-files/upload/${courseId}`,
            formData,
            {
                headers: {
                    'Content-Type': 'multipart/form-data',
                },
            }
        );
        return response.data;
    } catch (error) {
        throw new Error(error.response?.data?.message || 'Error uploading file');
    }
};

export default uploadCourseFile;
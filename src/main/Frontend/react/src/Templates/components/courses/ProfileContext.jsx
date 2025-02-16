import React, { createContext, useState } from 'react';

const ProfileContext = createContext();

export const ProfileProvider = ({ children }) => {
    const [profile, setProfile] = useState({ name: '', profilePicture: '' });

    const updateProfile = (teacherData) => {
        setProfile({
            name: teacherData.name || 'Unknown',
            profilePicture: teacherData.profilePicture || 'default-profile-picture-url',
        });
    };

    return (
        <ProfileContext.Provider value={{ profile, updateProfile }}>
            {children}
        </ProfileContext.Provider>
    );
};

